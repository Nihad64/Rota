// swift-tools-version: 6.0
import PackageDescription
let package=Package(name:"RotamDomain",platforms:[.macOS(.v13),.iOS(.v17)],products:[.library(name:"RotamDomain",targets:["RotamDomain"])],targets:[.executableTarget(name:"RotamContractCheck",dependencies:["RotamDomain"],path:"ContractCheck"),.target(name:"RotamDomain",path:"Rotam/Core"),.testTarget(name:"RotamDomainTests",dependencies:["RotamDomain"],path:"RotamTests",resources:[.process("Fixtures")])])
