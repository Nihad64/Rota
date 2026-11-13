#if DEBUG
import Foundation
import FirebaseCore
@preconcurrency import FirebaseAuth
@preconcurrency import FirebaseFirestore

struct EmulatorProbe:Sendable {let revision:Int64;let plans:Int;let preferences:Int;let rejectedOtherOwner:Bool}

extension FirebaseGateway {
 /** Compiled only for development; refuses any non-demo project before signing in. */
 func exerciseEmulatorContract()async throws->EmulatorProbe {
  guard let app=FirebaseApp.app(),app.options.projectID=="demo-rotam" else{throw RotamDataError.invalid("SDK sözleşme testi gerçek Firebase projesinde çalıştırılamaz.")}
  let email="contract-\(UUID().uuidString.lowercased())@rotam.example.test",password="Synthetic-only-Rotam-2026!"
  let owner=try await Auth.auth().createUser(withEmail:email,password:password).user.uid
  defer{try? Auth.auth().signOut();UserDefaults.standard.removeObject(forKey:"active-sync-uid");UserDefaults.standard.removeObject(forKey:"active-preferences-uid")}
  UserDefaults.standard.set(owner,forKey:"active-sync-uid")
  let name="Rotam-Secondary-\(UUID().uuidString)"
  let options=FirebaseOptions(googleAppID:"1:123456789:ios:abcdef0123456789",gcmSenderID:"123456789");options.projectID="demo-rotam";options.apiKey="demo-api-key"
  FirebaseApp.configure(name:name,options:options)
  guard let second=FirebaseApp.app(name:name)else{throw RotamDataError.invalid("İkinci SDK istemcisi açılamadı.")}
  let auth=Auth.auth(app:second),db=Firestore.firestore(app:second)
  auth.useEmulator(withHost:"127.0.0.1",port:9099);db.useEmulator(withHost:"127.0.0.1",port:8080)
  let secondOwner=try await auth.signIn(withEmail:email,password:password).user.uid
  guard secondOwner==owner else{throw RotamDataError.invalid("SDK hesapları uyuşmuyor.")}
  let seed=try PortablePlan.make(title:"Sentetik ortak plan",date:"2026-11-13",uid:"sdk-contract-plan").changing(["extraData":.string("{\"note\":\"İş / öğrenme\\nKorunmalı\",\"unknownFuture\":true}")])
  let revision=try await write(uid:owner,expected:0,records:[seed])
  guard revision==1 else{throw RotamDataError.invalid("İlk SDK sürümü doğrulanamadı.")}
  let ref=db.document("users/\(owner)/sync/snapshot"),remote=try await ref.getDocument(source:.server)
  guard let bytes=remote.data()?["payload"] as? Data,try SyncCodec.decode(bytes)==[seed]else{throw RotamDataError.invalid("İkinci SDK aynı planı okuyamadı.")}
  let edited=try seed.changing(["title":.string("İkinci SDK düzenledi")]),payload=try SyncCodec.encode([edited])
  _ = try await db.runTransaction{@Sendable tx,errorPointer->Any? in
   do{
    let snapshot=try tx.getDocument(ref);guard (snapshot.data()?["revision"] as? NSNumber)?.intValue==1 else{throw RotamDataError.invalid("İkinci istemcinin sürümü değişti.")}
    tx.setData(["schema":1,"revision":2,"writer":"synthetic-apple-client","updatedAt":FieldValue.serverTimestamp(),"payload":payload],forDocument:ref)
    return NSNumber(value:2)
   }catch{errorPointer?.pointee=error as NSError;return nil}
  }
  let readBack=try await read(uid:owner)
  guard readBack.revision==2,try readBack.index()==SyncEnvelope(records:[edited]).index()else{throw RotamDataError.invalid("Ters yöndeki SDK değişikliği doğrulanamadı.")}
  var staleRejected=false
  do{_ = try await write(uid:owner,expected:1,records:[seed])}catch{staleRejected=true}
  guard staleRejected else{throw RotamDataError.invalid("Eski SDK sürümü yeni kaydın üzerine yazdı.")}
  let preferences=try PortableSettings.encode(PortableSettings.defaults)
  var separateConsent=false
  do{_ = try await write(uid:owner,expected:0,records:preferences,preferences:true)}catch{separateConsent=true}
  guard separateConsent else{throw RotamDataError.invalid("Tercihler ayrı izin olmadan aktarıldı.")}
  UserDefaults.standard.set(owner,forKey:"active-preferences-uid")
  _ = try await write(uid:owner,expected:0,records:preferences,preferences:true)
  let prefDoc=try await db.document("users/\(owner)/preferences/snapshot").getDocument(source:.server)
  guard let prefBytes=prefDoc.data()?["payload"] as? Data,try PortableSettings.decode(SyncCodec.decode(prefBytes))==PortableSettings.defaults else{throw RotamDataError.invalid("SDK tercih paketi uyuşmuyor.")}
  try auth.signOut();_ = try await auth.createUser(withEmail:"other-\(UUID().uuidString)@rotam.example.test",password:password)
  var denied=false
  do{_ = try await ref.getDocument(source:.server)}catch{guard (error as NSError).code==7 else{throw error};denied=true}
  guard denied else{throw RotamDataError.invalid("Başka hesap SDK kaydını okuyabildi.")}
  return EmulatorProbe(revision:readBack.revision,plans:readBack.records.count,preferences:preferences.count,rejectedOtherOwner:denied)
 }
}
#endif
