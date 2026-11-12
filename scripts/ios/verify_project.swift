import Foundation
let p="ios/Rotam.xcodeproj/project.pbxproj"
let root=try PropertyListSerialization.propertyList(from:Data(contentsOf:URL(fileURLWithPath:p)),options:[],format:nil) as! [String:Any]
let objects=root["objects"] as! [String:[String:Any]]
let app=objects.values.first{$0["isa"] as? String=="PBXNativeTarget"&&$0["name"] as? String=="Rotam"}!
let linked=(app["packageProductDependencies"] as? [String] ?? []).compactMap{objects[$0]?["productName"] as? String}
let expected=Set(["FirebaseCore","FirebaseAuth","FirebaseFirestore","FirebaseAppCheck","FirebaseAILogic","GoogleSignIn"])
precondition(Set(linked).isSuperset(of:expected),"Service packages must belong to the app target")
for t in objects.values where t["isa"] as? String=="PBXNativeTarget"&&t["name"] as? String != "Rotam"{
 let deps=(t["packageProductDependencies"] as? [String] ?? []).compactMap{objects[$0]?["productName"] as? String}
 precondition(Set(deps).isDisjoint(with:expected),"App services were linked to an unrelated target")
}
precondition((app["fileSystemSynchronizedGroups"] as? [String])?.isEmpty==false)
let configs=objects[app["buildConfigurationList"] as! String]!["buildConfigurations"] as! [String]
for c in configs{let s=objects[c]!["buildSettings"] as! [String:Any];precondition((s["OTHER_LDFLAGS"] as? [String] ?? []).contains("-ObjC"))}
print("App package links, synchronized folders and Objective-C categories verified")
