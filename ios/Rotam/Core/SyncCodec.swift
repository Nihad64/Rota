import Foundation
import zlib

public enum SyncCodec {
 public static let maxCompressed=700*1024,maxExpanded=20*1024*1024
 private static func transform(_ input:Data,compress:Bool)throws->Data{
  guard !input.isEmpty,input.count<=(compress ? maxExpanded:maxCompressed)else{throw RotamDataError.invalid("Eşitleme boyut sınırı aşıldı. Cihaz kayıtların korunuyor.")}
  return try input.withUnsafeBytes{bytes in
   var stream=z_stream()
   let initialized=compress ? deflateInit2_(&stream,Z_DEFAULT_COMPRESSION,Z_DEFLATED,31,8,Z_DEFAULT_STRATEGY,ZLIB_VERSION,Int32(MemoryLayout<z_stream>.size)):inflateInit2_(&stream,47,ZLIB_VERSION,Int32(MemoryLayout<z_stream>.size))
   guard initialized==Z_OK else{throw RotamDataError.invalid("Sıkıştırma başlatılamadı.")}
   defer{if compress{deflateEnd(&stream)}else{inflateEnd(&stream)}}
   stream.next_in=UnsafeMutablePointer(mutating:bytes.bindMemory(to:Bytef.self).baseAddress);stream.avail_in=UInt32(input.count)
   var output=Data();var buffer=[UInt8](repeating:0,count:8192)
   while true{
    let result=buffer.withUnsafeMutableBytes{ptr->Int32 in stream.next_out=ptr.bindMemory(to:Bytef.self).baseAddress;stream.avail_out=8192;return compress ? deflate(&stream,Z_FINISH):inflate(&stream,Z_NO_FLUSH)}
    let written=8192-Int(stream.avail_out)
    guard output.count+written<=(compress ? maxCompressed:maxExpanded)else{throw RotamDataError.invalid("Açılan bulut verisi sınırı aşıyor.")}
    output.append(contentsOf:buffer.prefix(written))
    if result==Z_STREAM_END{guard stream.avail_in==0 else{throw RotamDataError.invalid("Bulut paketinde beklenmeyen ek veri var.")};break}
    guard result==Z_OK && (written>0 || stream.avail_in>0)else{throw RotamDataError.invalid("Bulut paketi eksik veya bozuk.")}
   }
   return output
  }
 }
 public static func encode(_ records:[WireRecord])throws->Data{
  let envelope:JSONValue = .object(["schema":.integer(records.contains{$0.kind == .preference} ? 3:2),"records":.array(records.sorted{($0.kind.rawValue,$0.uid)<($1.kind.rawValue,$1.uid)}.map{.object(["kind":.string($0.kind.rawValue),"uid":.string($0.uid),"body":.string($0.body)])})])
  return try transform(Data(envelope.canonical().utf8),compress:true)
 }
 public static func decode(_ data:Data)throws->[WireRecord]{
  let raw=try transform(data,compress:false)
  let root=try JSONDecoder().decode(JSONValue.self,from:raw)
  guard let d=root.fields,let version=d["schema"]?.integer,(1...3).contains(version),let rows=d["records"]?.items,rows.count<=100000 else{throw RotamDataError.invalid("Bulut sürümü desteklenmiyor. Kayıtların korunuyor.")}
  let records=try rows.map{row->WireRecord in
   guard let d=row.fields,let k=d["kind"]?.string.flatMap(WireKind.init(rawValue:)),let uid=d["uid"]?.string,let body=d["body"]?.string,version>=3 || k != .preference else{throw RotamDataError.invalid("Bulut kayıt türü desteklenmiyor.")}
   return try WireRecord(kind:k,uid:uid,body:body)
  }
  _ = try SyncEnvelope(records:records).index();return records
 }
}
