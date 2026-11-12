import Foundation

public enum RotamDataError:LocalizedError,Sendable {
 case invalid(String)
 public var errorDescription:String?{switch self{case .invalid(let message):return message}}
}
public enum JSONValue:Codable,Hashable,Sendable {
 case object([String:JSONValue]),array([JSONValue]),string(String),integer(Int64),number(Double),bool(Bool),null
 public init(from decoder:any Decoder)throws{
  let c=try decoder.singleValueContainer()
  if c.decodeNil(){self = .null}
  else if let b=try? c.decode(Bool.self){self = .bool(b)}
  else if let n=try? c.decode(Int64.self){self = .integer(n)}
  else if let n=try? c.decode(Double.self),n.isFinite{self = .number(n)}
  else if let s=try? c.decode(String.self){self = .string(s)}
  else if let a=try? c.decode([JSONValue].self){self = .array(a)}
  else{self = .object(try c.decode([String:JSONValue].self))}
 }
 public func encode(to encoder:any Encoder)throws{
  var c=encoder.singleValueContainer()
  switch self{case .null:try c.encodeNil();case .bool(let b):try c.encode(b);case .integer(let n):try c.encode(n);case .number(let n):try c.encode(n);case .string(let s):try c.encode(s);case .array(let a):try c.encode(a);case .object(let d):try c.encode(d)}
 }
 public var string:String?{if case .string(let s)=self{return s};return nil}
 public var integer:Int64?{if case .integer(let n)=self{return n};return nil}
 public var boolean:Bool?{if case .bool(let b)=self{return b};return nil}
 public var fields:[String:JSONValue]?{if case .object(let d)=self{return d};return nil}
 public var items:[JSONValue]?{if case .array(let a)=self{return a};return nil}
 public static func parse(_ text:String)throws->JSONValue{try JSONDecoder().decode(JSONValue.self,from:Data(text.utf8))}
 public func canonical()throws->String{
  switch self{
  case .object(let d):return "{"+(try d.keys.sorted().map{try JSONValue.string($0).canonical()+":"+d[$0]!.canonical()}).joined(separator:",")+"}"
  case .array(let a):return "["+(try a.map{try $0.canonical()}).joined(separator:",")+"]"
  default:let e=JSONEncoder();return String(decoding:try e.encode(self),as:UTF8.self)
  }
 }
}
public enum WireKind:String,Codable,Sendable,CaseIterable{case plan="PLAN",series="SERIES",goal="GOAL",memory="MEMORY",preference="PREFERENCE"}
public struct WireKey:Hashable,Codable,Sendable {public let kind:WireKind;public let uid:String;public init(_ kind:WireKind,_ uid:String){self.kind=kind;self.uid=uid}}
public struct WireRecord:Hashable,Codable,Sendable {
 public let kind:WireKind;public let uid:String;public let body:String
 public var key:WireKey{WireKey(kind,uid)}
 public init(kind:WireKind,uid:String,body:String)throws{
  guard !uid.isEmpty,uid.utf16.count<=512,body.utf8.count<=256*1024,let d=try JSONValue.parse(body).fields,d["uid"]?.string==uid else{throw RotamDataError.invalid("Kayıt kimliği veya biçimi doğrulanamadı.")}
  self.kind=kind;self.uid=uid;self.body=body
 }
 public init(kind:WireKind,fields:[String:JSONValue])throws{try self.init(kind:kind,uid:fields["uid"]?.string ?? "",body:JSONValue.object(fields).canonical())}
 public func fields()throws->[String:JSONValue]{guard let d=try JSONValue.parse(body).fields else{throw RotamDataError.invalid("Kayıt biçimi desteklenmiyor.")};return d}
 public func changing(_ changes:[String:JSONValue])throws->WireRecord{var d=try fields();for(k,v)in changes{d[k]=v};return try WireRecord(kind:kind,fields:d)}
}
public struct SyncEnvelope:Codable,Sendable,Equatable {
 public var revision:Int64;public var records:[WireRecord]
 public init(revision:Int64=0,records:[WireRecord]=[]){self.revision=revision;self.records=records}
 public func index()throws->[WireKey:WireRecord]{var d:[WireKey:WireRecord]=[:];for r in records{guard d.updateValue(r,forKey:r.key)==nil else{throw RotamDataError.invalid("Yinelenen bulut kimliği var.")}};return d}
}
public struct WireConflict:Sendable,Identifiable {public var id:WireKey{key};public let key:WireKey;public let base:WireRecord?;public let local:WireRecord?;public let remote:WireRecord?}
public struct WireMerge:Sendable {
 public var records:[WireKey:WireRecord];public var conflicts:[WireConflict]
 public func choosingLocal(_ local:Bool)->[WireKey:WireRecord]{var d=records;for c in conflicts{d[c.key]=local ? c.local:c.remote};return d}
}
public enum ThreeWayMerge {
 public static func merge(base:[WireKey:WireRecord],local:[WireKey:WireRecord],remote:[WireKey:WireRecord])->WireMerge{
  var records:[WireKey:WireRecord]=[:];var conflicts:[WireConflict]=[]
  for key in Set(base.keys).union(local.keys).union(remote.keys){
   let b=base[key],l=local[key],r=remote[key]
   if l==r{records[key]=l}else if l==b{records[key]=r}else if r==b{records[key]=l}else{records[key]=l;conflicts.append(WireConflict(key:key,base:b,local:l,remote:r))}
  }
  return WireMerge(records:records,conflicts:conflicts.sorted{$0.key.uid<$1.key.uid})
 }
}
