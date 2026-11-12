import SwiftUI
import CoreText

enum Brand {
 static let background=Color(red:0.047,green:0.055,blue:0.059)
 static let surface=Color(red:0.125,green:0.137,blue:0.149)
 static let raised=Color(red:0.16,green:0.175,blue:0.19)
 static let ink=Color(red:0.97,green:0.95,blue:0.90)
 static let muted=Color(red:0.68,green:0.70,blue:0.73)
 static let orange=Color(red:1,green:0.50,blue:0.247)
 static let line=Color.white.opacity(0.14)
 static func icon(_ category:String)->String{["kitap":"book","is":"briefcase","video":"play.rectangle","spor":"dumbbell","ibadet":"moon.stars","randevu":"calendar.badge.clock","yemek":"fork.knife","ogrenme":"graduationcap"] [category] ?? "sparkles"}
}
struct BrandType:ViewModifier {
 @ScaledMetric var size:CGFloat
 let weight:CGFloat
 init(_ size:CGFloat,weight:CGFloat){_size=ScaledMetric(wrappedValue:size,relativeTo:size>=28 ? .title:.body);self.weight=weight}
 func body(content:Content)->some View {
  let descriptor=UIFontDescriptor(fontAttributes:[.name:"Manrope-ExtraLight",UIFontDescriptor.AttributeName(rawValue:kCTFontVariationAttribute as String):[NSNumber(value:2003265652):NSNumber(value:Double(weight))]])
  return content.font(Font(UIFont(descriptor:descriptor,size:size)))
 }
}
extension View {
 func brandFont(_ size:CGFloat=17,weight:CGFloat=500)->some View{modifier(BrandType(size,weight:weight))}
 func surfaceCard()->some View{padding(20).frame(maxWidth:.infinity,alignment:.leading).background(Brand.surface,in:RoundedRectangle(cornerRadius:28)).overlay(RoundedRectangle(cornerRadius:28).strokeBorder(Brand.line.opacity(0.5)))}
}
struct PrimaryButton:ButtonStyle {
 func makeBody(configuration:Configuration)->some View{configuration.label.brandFont(17,weight:800).foregroundStyle(Brand.background).padding(.horizontal,20).padding(.vertical,16).frame(maxWidth:.infinity).background(Brand.orange,in:Capsule()).opacity(configuration.isPressed ? 0.78:1).scaleEffect(configuration.isPressed ? 0.98:1)}
}
struct QuietButton:ButtonStyle {
 func makeBody(configuration:Configuration)->some View{configuration.label.brandFont(16,weight:700).foregroundStyle(Brand.ink).padding(.horizontal,18).padding(.vertical,13).frame(minHeight:48).background(configuration.isPressed ? Brand.raised:Color.clear,in:Capsule()).overlay(Capsule().strokeBorder(Brand.line))}
}
struct BrandMark:View{var body:some View{HStack(spacing:10){Image("BrandMark").resizable().scaledToFit().frame(width:34,height:38);Text("rotam").brandFont(30,weight:800)}.accessibilityElement(children:.ignore).accessibilityLabel("Rotam")}}
struct Diamond:View{var body:some View{RoundedRectangle(cornerRadius:3).fill(Brand.orange).frame(width:15,height:15).rotationEffect(.degrees(45)).accessibilityHidden(true)}}
struct Cutout:Shape {
 func path(in r:CGRect)->Path{
  let box=UIBezierPath(roundedRect:r,cornerRadius:32);let cut=UIBezierPath(ovalIn:CGRect(x:r.maxX-90,y:r.maxY-90,width:125,height:125));box.append(cut);box.usesEvenOddFillRule=true;return Path(box.cgPath)
 }
}
struct HeroCard<Content:View,Action:View>:View {
 @ViewBuilder var content:Content
 @ViewBuilder var action:Action
 var body:some View{ZStack(alignment:.bottomTrailing){content.padding(26).padding(.bottom,40).frame(maxWidth:.infinity,alignment:.leading).background{Cutout().fill(LinearGradient(colors:[Brand.raised,Brand.surface],startPoint:.topLeading,endPoint:.bottomTrailing),style:FillStyle(eoFill:true))};action.offset(x:-5,y:2)}.padding(.bottom,12)}
}
struct SectionTitle:View{let title:String;var body:some View{Text(title).brandFont(23,weight:800).frame(maxWidth:.infinity,alignment:.leading).padding(.top,12)}}
struct PlanRow:View {
 let plan:PortablePlan
 var body:some View{HStack(alignment:.top,spacing:16){Image(systemName:Brand.icon(plan.category)).font(.title2).frame(width:30).foregroundStyle(plan.completed ? Brand.orange:Brand.ink);VStack(alignment:.leading,spacing:6){Text(plan.title).brandFont(19,weight:750).foregroundStyle(Brand.ink);Text(plan.scheduleLabel).brandFont(14).foregroundStyle(Brand.muted);if plan.completed{Label("Tamamlandı",systemImage:"checkmark.circle.fill").brandFont(13).foregroundStyle(Brand.orange)}else if plan.outcome=="PARTIAL"{Text("Kısmen tamamlandı").brandFont(13).foregroundStyle(Brand.orange)}};Spacer(minLength:0);Image(systemName:"chevron.right").font(.footnote).foregroundStyle(Brand.muted)}.padding(.vertical,16).contentShape(Rectangle())}
}
struct EmptyState:View {
 let symbol:String;let title:String;let detail:String
 var body:some View{VStack(alignment:.leading,spacing:14){Image(systemName:symbol).font(.largeTitle).foregroundStyle(Brand.orange);Text(title).brandFont(25,weight:800);Text(detail).brandFont(16).foregroundStyle(Brand.muted).fixedSize(horizontal:false,vertical:true)}.surfaceCard()}
}
