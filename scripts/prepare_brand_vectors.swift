// Trace the approved logo's existing silhouette into native code assets.
// No replacement logo is generated. The user's later pure-white decision overrides ivory.
import AppKit
import Foundation
let root=URL(fileURLWithPath:FileManager.default.currentDirectoryPath)
let mask=NSBitmapImageRep(data:try Data(contentsOf:root.appendingPathComponent("app/src/main/res/drawable-nodpi/rotam_launcher_mask.png")))!
let size=1024
precondition(mask.pixelsWide==size&&mask.pixelsHigh==size)
var on=[Bool](repeating:false,count:size*size)
for y in 0..<size {for x in 0..<size {on[y*size+x]=mask.colorAt(x:x,y:y)!.alphaComponent>=0.5}}
var seen=[Bool](repeating:false,count:on.count)
var shapes:[(pixels:[Int],loops:[[Int]])]=[]
let stride=size+1
for seed in 0..<on.count where on[seed] && !seen[seed] {
 var pixels=[Int]();var pending=[seed];seen[seed]=true
 while let p=pending.popLast(){pixels.append(p);let x=p%size;let y=p/size
  for (nx,ny) in [(x-1,y),(x+1,y),(x,y-1),(x,y+1)] where nx>=0&&nx<size&&ny>=0&&ny<size {
   let next=ny*size+nx;if on[next] && !seen[next]{seen[next]=true;pending.append(next)}
  }
 }
 guard pixels.count>100 else {continue}
 var edges=[Int:[Int]]()
 func add(_ x:Int,_ y:Int,_ xx:Int,_ yy:Int){edges[y*stride+x,default:[]].append(yy*stride+xx)}
 func filled(_ x:Int,_ y:Int)->Bool{x>=0&&y>=0&&x<size&&y<size&&on[y*size+x]}
 for p in pixels {let x=p%size;let y=p/size
  if !filled(x,y-1){add(x,y,x+1,y)}
  if !filled(x+1,y){add(x+1,y,x+1,y+1)}
  if !filled(x,y+1){add(x+1,y+1,x,y+1)}
  if !filled(x-1,y){add(x,y+1,x,y)}
 }
 var loops=[[Int]]()
 while let start=edges.keys.min(){var path=[start];var cursor=start
  repeat {
   guard var candidates=edges[cursor],let next=candidates.popLast() else {fatalError("Open silhouette")}
   if candidates.isEmpty{edges.removeValue(forKey:cursor)}else{edges[cursor]=candidates}
   cursor=next;if cursor != start{path.append(cursor)}
  }while cursor != start
  // Preserve the pixel contour exactly while removing redundant collinear points.
  path=path.indices.compactMap{i in
   let a=path[(i+path.count-1)%path.count];let b=path[i];let c=path[(i+1)%path.count]
   let ax=a%stride,ay=a/stride,bx=b%stride,by=b/stride,cx=c%stride,cy=c/stride
   return (bx-ax)*(cy-by)==(by-ay)*(cx-bx) ? nil:b
  }
  loops.append(path)
 }
 shapes.append((pixels,loops))
}
shapes.sort{$0.pixels.count>$1.pixels.count}
precondition(shapes.count==2,"Expected the R and the orange diamond only")
func data(_ loops:[[Int]],_ dx:Int=0,_ dy:Int=0)->String {
 loops.map{path in path.enumerated().map{i,p in "\(i==0 ? "M":"L")\(p%stride-dx),\(p/stride-dy)"}.joined()+"Z"}.joined()
}
func vector(_ width:Int,_ height:Int,_ dx:Int=0,_ dy:Int=0)->String {
 let paths=shapes.enumerated().map{i,s in "    <path android:fillColor=\"\(i==0 ? "#FFFFFFFF":"#FFFF803F")\" android:fillType=\"evenOdd\" android:pathData=\"\(data(s.loops,dx,dy))\" />"}.joined(separator:"\n")
 return "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" android:width=\"108dp\" android:height=\"108dp\" android:viewportWidth=\"\(width)\" android:viewportHeight=\"\(height)\">\n\(paths)\n</vector>\n"
}
try vector(1024,1024).write(to:root.appendingPathComponent("app/src/main/res/drawable/ic_launcher_foreground.xml"),atomically:true,encoding:.utf8)
try vector(485,520,280,250).write(to:root.appendingPathComponent("app/src/main/res/drawable/rotam_mark_vector.xml"),atomically:true,encoding:.utf8)
print("Generated original-silhouette vectors: R=\(shapes[0].pixels.count)px, diamond=\(shapes[1].pixels.count)px; white #FFFFFF, orange #FF803F")
// Export the same native vector paths for iOS brand assets; no iOS runtime is implied.
let svgPaths=shapes.enumerated().map{i,s in "<path fill=\"\(i==0 ? "#FFFFFF":"#FF803F")\" fill-rule=\"evenodd\" d=\"\(data(s.loops))\"/>"}.joined()
try "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1024 1024\"><rect width=\"1024\" height=\"1024\" fill=\"#000000\"/>\(svgPaths)</svg>\n".write(to:root.appendingPathComponent("ios/Brand.svg"),atomically:true,encoding:.utf8)
let rendered=NSBitmapImageRep(bitmapDataPlanes:nil,pixelsWide:1024,pixelsHigh:1024,bitsPerSample:8,samplesPerPixel:4,hasAlpha:true,isPlanar:false,colorSpaceName:.deviceRGB,bytesPerRow:4096,bitsPerPixel:32)!
NSGraphicsContext.saveGraphicsState()
let graphics=NSGraphicsContext(bitmapImageRep:rendered)!
NSGraphicsContext.current=graphics
NSColor.black.setFill();NSRect(x:0,y:0,width:1024,height:1024).fill()
graphics.cgContext.translateBy(x:0,y:1024);graphics.cgContext.scaleBy(x:1,y:-1)
for (index,shape) in shapes.enumerated(){
 let path=NSBezierPath();path.windingRule = .evenOdd
 for loop in shape.loops {for (i,point) in loop.enumerated(){let p=NSPoint(x:point%stride,y:point/stride);if i==0{path.move(to:p)}else{path.line(to:p)}};path.close()}
 (index==0 ? NSColor.white:NSColor(deviceRed:1,green:128.0/255,blue:63.0/255,alpha:1)).setFill();path.fill()
}
NSGraphicsContext.restoreGraphicsState()
try rendered.representation(using:.png,properties:[:])!.write(to:root.appendingPathComponent("ios/Brand.xcassets/AppIcon.appiconset/Rotam-1024.png"))
