import Foundation
// Creates the initial project only. Sources/resources are directory synchronized;
// later dependency edits use the Firebase skill's native Swift package tool.
let base=URL(fileURLWithPath:FileManager.default.currentDirectoryPath).appendingPathComponent("ios/Rotam.xcodeproj")
if FileManager.default.fileExists(atPath:base.path){print("Existing project retained");exit(0)}
var objects:[String:[String:Any]]=[:]
func id(_ n:Int)->String{String(format:"%024X",n)}
@discardableResult func add(_ n:Int,_ v:[String:Any])->String{objects[id(n)]=v;return id(n)}
func encode(_ v:Any,_ depth:Int=0)->String{
 let tab=String(repeating:"\t",count:depth)
 if let d=v as? [String:Any]{return "{\n"+d.keys.sorted().map{tab+"\t\($0) = \(encode(d[$0]!,depth+1));"}.joined(separator:"\n")+"\n"+tab+"}"}
 if let a=v as? [Any]{return a.isEmpty ? "()":"("+a.map{encode($0,depth)}.joined(separator:", ")+",)"}
 if let n=v as? Int{return String(n)}
 return "\""+(v as! String).replacingOccurrences(of:"\\",with:"\\\\").replacingOccurrences(of:"\"",with:"\\\"").replacingOccurrences(of:"\n",with:"\\n")+"\""
}
func configs(_ n:Int,_ values:[String:Any])->String{
 let children=["Debug","Release"].enumerated().map{i,name->String in
  var s=values;s["SWIFT_OPTIMIZATION_LEVEL"]=i==0 ? "-Onone":"-O"
  if i==0{s["SWIFT_ACTIVE_COMPILATION_CONDITIONS"]="$(inherited) DEBUG";s["ONLY_ACTIVE_ARCH"]="YES";s["ENABLE_TESTABILITY"]="YES"}
  return add(n+i,["isa":"XCBuildConfiguration","name":name,"buildSettings":s])
 }
 return add(n+2,["isa":"XCConfigurationList","buildConfigurations":children,"defaultConfigurationIsVisible":0,"defaultConfigurationName":"Release"])
}
let global=configs(100,["SDKROOT":"iphoneos","IPHONEOS_DEPLOYMENT_TARGET":"17.0","SWIFT_VERSION":"6.0","CLANG_ENABLE_MODULES":"YES","ENABLE_USER_SCRIPT_SANDBOXING":"YES","CODE_SIGN_STYLE":"Automatic","TARGETED_DEVICE_FAMILY":"1,2","MARKETING_VERSION":"3.1.0","CURRENT_PROJECT_VERSION":"46"])
let groups=[(20,"Rotam"),(21,"RotamTests"),(22,"RotamUITests"),(23,"RotamWidget"),(24,"Rotam/Core")]
for (n,path) in groups{
 var s:[String:Any]=["isa":"PBXFileSystemSynchronizedRootGroup","path":path,"sourceTree":"<group>"]
 if n==20||n==23{s["exceptions"]=[id(n==20 ? 25:26)]}
 add(n,s)
}
add(25,["isa":"PBXFileSystemSynchronizedBuildFileExceptionSet","target":id(1),"membershipExceptions":["Info.plist","Rotam.entitlements"]])
add(26,["isa":"PBXFileSystemSynchronizedBuildFileExceptionSet","target":id(4),"membershipExceptions":["Info.plist","RotamWidget.entitlements"]])
let names=["Rotam","RotamTests","RotamUITests","RotamWidget"]
let types=["application","bundle.unit-test","bundle.ui-testing","app-extension"]
let suffixes=["app","xctest","xctest","appex"]
for i in 0..<4{
 let n=i+1;let name=names[i]
 add(30+n,["isa":"PBXFileReference","explicitFileType":i==0 ? "wrapper.application":i==3 ? "wrapper.app-extension":"wrapper.cfbundle","path":"\(name).\(suffixes[i])","sourceTree":"BUILT_PRODUCTS_DIR"])
 for (offset,kind) in [(40,"PBXSourcesBuildPhase"),(50,"PBXResourcesBuildPhase"),(60,"PBXFrameworksBuildPhase")]{add(offset+n,["isa":kind,"buildActionMask":2147483647,"files":[],"runOnlyForDeploymentPostprocessing":0])}
 var settings:[String:Any]=["PRODUCT_NAME":"$(TARGET_NAME)","PRODUCT_BUNDLE_IDENTIFIER":i==0 ? "com.rota.app":"com.rota.app.\(i==3 ? "widget":i==2 ? "uitests":"tests")","GENERATE_INFOPLIST_FILE":"YES"]
 if i==0||i==3{settings["INFOPLIST_FILE"]="\(name)/Info.plist";settings["CODE_SIGN_ENTITLEMENTS"]="\(name)/\(name).entitlements"}
 if i==0{settings["ASSETCATALOG_COMPILER_APPICON_NAME"]="AppIcon";settings["OTHER_LDFLAGS"]=["$(inherited)","-ObjC"]}
 if i==1{settings["TEST_HOST"]="$(BUILT_PRODUCTS_DIR)/Rotam.app/Rotam";settings["BUNDLE_LOADER"]="$(TEST_HOST)"}
 if i==2{settings["TEST_TARGET_NAME"]="Rotam"}
 if i==3{settings["APPLICATION_EXTENSION_API_ONLY"]="YES";settings["SKIP_INSTALL"]="YES"}
 let configuration=configs(200+n*10,settings)
 add(n,["isa":"PBXNativeTarget","name":name,"productName":name,"productType":"com.apple.product-type.\(types[i])","productReference":id(30+n),"buildConfigurationList":configuration,"buildPhases":[id(40+n),id(60+n),id(50+n)],"buildRules":[],"dependencies":[],"fileSystemSynchronizedGroups":i==3 ? [id(23),id(24)]:[id(20+i)],"packageProductDependencies":[]])
}
for n in [1,2,3]{
 let target=n==1 ? 4:1
 let proxy=add(400+n,["isa":"PBXContainerItemProxy","containerPortal":id(10),"proxyType":1,"remoteGlobalIDString":id(target),"remoteInfo":names[target-1]])
 objects[id(n)]!["dependencies"]=[add(410+n,["isa":"PBXTargetDependency","target":id(target),"targetProxy":proxy])]
}
add(430,["isa":"PBXBuildFile","fileRef":id(34),"settings":["ATTRIBUTES":["RemoveHeadersOnCopy"]]])
let embed=add(431,["isa":"PBXCopyFilesBuildPhase","buildActionMask":2147483647,"dstPath":"","dstSubfolderSpec":13,"files":[id(430)],"name":"Embed App Extensions","runOnlyForDeploymentPostprocessing":0])
objects[id(1)]!["buildPhases"]=(objects[id(1)]!["buildPhases"] as! [String])+[embed]
add(70,["isa":"PBXGroup","name":"Products","children":(31...34).map{id($0)},"sourceTree":"<group>"])
add(71,["isa":"PBXGroup","children":groups.map{id($0.0)}+[id(70)],"sourceTree":"<group>"])
add(10,["isa":"PBXProject","attributes":["LastUpgradeCheck":"2660","LastSwiftUpdateCheck":"2660","BuildIndependentTargetsInParallel":"YES","TargetAttributes":[id(2):["TestTargetID":id(1)],id(3):["TestTargetID":id(1)]]],"buildConfigurationList":global,"compatibilityVersion":"Xcode 16.0","developmentRegion":"tr","knownRegions":["tr","en","Base"],"mainGroup":id(71),"productRefGroup":id(70),"projectDirPath":"","projectRoot":"","targets":(1...4).map{id($0)},"preferredProjectObjectVersion":77,"packageReferences":[]])
let document:[String:Any]=["archiveVersion":1,"classes":[:],"objectVersion":77,"objects":objects,"rootObject":id(10)]
try FileManager.default.createDirectory(at:base,withIntermediateDirectories:true)
try ("// !$*UTF8*$!\n"+encode(document)+"\n").write(to:base.appendingPathComponent("project.pbxproj"),atomically:true,encoding:.utf8)
let directory=base.appendingPathComponent("xcshareddata/xcschemes");try FileManager.default.createDirectory(at:directory,withIntermediateDirectories:true)
func ref(_ n:Int)->String{"<BuildableReference BuildableIdentifier=\"primary\" BlueprintIdentifier=\"\(id(n))\" BuildableName=\"\(names[n-1]).\(suffixes[n-1])\" BlueprintName=\"\(names[n-1])\" ReferencedContainer=\"container:Rotam.xcodeproj\"/>"}
let scheme="""
<?xml version="1.0" encoding="UTF-8"?>
<Scheme LastUpgradeVersion="2660" version="1.7"><BuildAction parallelizeBuildables="YES" buildImplicitDependencies="YES"><BuildActionEntries><BuildActionEntry buildForTesting="YES" buildForRunning="YES" buildForProfiling="YES" buildForArchiving="YES" buildForAnalyzing="YES">\(ref(1))</BuildActionEntry></BuildActionEntries></BuildAction><TestAction buildConfiguration="Debug" selectedDebuggerIdentifier="Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier="Xcode.IDEFoundation.Launcher.LLDB" shouldUseLaunchSchemeArgsEnv="YES"><Testables><TestableReference skipped="NO">\(ref(2))</TestableReference><TestableReference skipped="NO">\(ref(3))</TestableReference></Testables></TestAction><LaunchAction buildConfiguration="Debug" selectedDebuggerIdentifier="Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier="Xcode.IDEFoundation.Launcher.LLDB" launchStyle="0" useCustomWorkingDirectory="NO" ignoresPersistentStateOnLaunch="NO" debugDocumentVersioning="YES" debugServiceExtension="internal" allowLocationSimulation="YES"><BuildableProductRunnable runnableDebuggingMode="0">\(ref(1))</BuildableProductRunnable></LaunchAction><ProfileAction buildConfiguration="Release" shouldUseLaunchSchemeArgsEnv="YES" savedToolIdentifier="" useCustomWorkingDirectory="NO" debugDocumentVersioning="YES"><BuildableProductRunnable runnableDebuggingMode="0">\(ref(1))</BuildableProductRunnable></ProfileAction><AnalyzeAction buildConfiguration="Debug"/><ArchiveAction buildConfiguration="Release" revealArchiveInOrganizer="YES"/></Scheme>
"""
try scheme.write(to:directory.appendingPathComponent("Rotam.xcscheme"),atomically:true,encoding:.utf8)
print("Created directory-synchronized app, tests, and widget targets")
