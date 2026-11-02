package com.nihad64.rotam.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.nihad64.rotam.*
import com.nihad64.rotam.ui.ai.*
import com.nihad64.rotam.ui.calendar.*
import com.nihad64.rotam.ui.components.*
import com.nihad64.rotam.ui.goals.*
import com.nihad64.rotam.ui.home.*
import com.nihad64.rotam.ui.newplan.*
import com.nihad64.rotam.ui.settings.*
import com.nihad64.rotam.ui.stats.*
import com.nihad64.rotam.ui.focus.*
import com.nihad64.rotam.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
private data class Tab(val route:String,val label:String,val icon:ImageVector)
private val tabs=listOf(Tab("home","Bugün",Icons.Outlined.Home),Tab("calendar","Takvim",Icons.Outlined.CalendarMonth),Tab("goals","Hedefler",Icons.Outlined.Adjust),Tab("settings","Ayarlar",Icons.Outlined.Settings))
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun RotamRoot(){
 val settings by RotamApp.container.settings.settings.collectAsStateWithLifecycle(initialValue=com.nihad64.rotam.data.prefs.Settings())
 CompositionLocalProvider(LocalRotamSettings provides settings){RotamContent()}
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun RotamContent(){
 val c=RotamApp.container;val ready by c.ready.collectAsStateWithLifecycle();val error by c.startupError.collectAsStateWithLifecycle();val scope=rememberCoroutineScope()
 if(!ready){Box(Modifier.fillMaxSize().background(Background).safeDrawingPadding().padding(28.dp),contentAlignment=Alignment.Center){if(error==null)CircularProgressIndicator()else Column(verticalArrangement=Arrangement.spacedBy(20.dp)){Text(error!!);PrimaryButton("Yeniden dene",{scope.launch{runCatching{c.initialize()}}})}};return}
 val nav=rememberNavController();val entry by nav.currentBackStackEntryAsState();val route=entry?.destination?.route
 var aiOpen by remember{mutableStateOf(false)};val ai:AiViewModel=viewModel{AiViewModel(c)}
 val snackbar=remember{SnackbarHostState()};val showBar=route in tabs.map{it.route}
 val context=LocalContext.current
 LaunchedEffect(Unit){val id=(context as? android.app.Activity)?.intent?.getLongExtra("planId",-1)?:-1;if(id>0)nav.navigate("edit/$id")else if((context as? android.app.Activity)?.intent?.getBooleanExtra("newPlan",false)==true)nav.navigate("new?date=&copy=-1")}
 LaunchedEffect(Unit){for(n in c.notices){val result=snackbar.showSnackbar(n.text,if(n.undo!=null)"Geri al"else null,duration=SnackbarDuration.Long);if(result==SnackbarResult.ActionPerformed)runCatching{n.undo?.invoke()}}}
 fun new(date:LocalDate?=null){nav.navigate("new?date=${date?:""}&copy=-1")}
 Scaffold(containerColor=Background,contentWindowInsets=WindowInsets.safeDrawing,
  snackbarHost={SnackbarHost(snackbar)},bottomBar={if(showBar)Row(Modifier.fillMaxWidth().background(Background).navigationBarsPadding().padding(vertical=10.dp)){
   tabs.forEach{t->val selected=t.route==route;Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable{nav.navigate(t.route){popUpTo("home"){saveState=true};launchSingleTop=true;restoreState=true}}.padding(vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(t.icon,t.label,tint=if(selected)Primary else SubtleText,modifier=Modifier.size(25.dp));Text(t.label,style=MaterialTheme.typography.labelSmall,color=if(selected)Primary else SubtleText,modifier=Modifier.padding(top=5.dp));Spacer(Modifier.height(7.dp));Box(Modifier.size(4.dp).clip(CircleShape).background(if(selected)Accent else Background))}}
  }},floatingActionButton={if(!route.orEmpty().startsWith("focus"))SmallAiButton{aiOpen=true}}){padding->
  NavHost(nav,startDestination="home",modifier=Modifier.padding(padding),enterTransition={androidx.compose.animation.EnterTransition.None},exitTransition={androidx.compose.animation.ExitTransition.None}){
   composable("home"){HomeScreen(viewModel{HomeViewModel(c)},{new()},{nav.navigate("edit/$it")},{nav.navigate("focus/$it")},{nav.navigate("search")},{nav.navigate("stats")},{nav.navigate("settings")})}
   composable("calendar"){val vm:CalendarViewModel=viewModel{CalendarViewModel(c)};CalendarScreen(vm,{new(vm.state.value.selected)},{nav.navigate("edit/$it")})}
   composable("goals"){GoalsScreen(viewModel{GoalsViewModel(c)},onBack={nav.navigate("home")})}
   composable("settings"){SettingsScreen(viewModel{SettingsViewModel(c)},onStats={nav.navigate("stats")},onSearch={nav.navigate("search")})}
   composable("stats"){StatsScreen(viewModel{StatsViewModel(c)},onBack={nav.popBackStack()})}
   composable("search"){SearchScreen(c,{nav.popBackStack()},{nav.navigate("edit/$it")})}
   composable("new?date={date}&copy={copy}",arguments=listOf(navArgument("date"){defaultValue=""},navArgument("copy"){type=NavType.LongType;defaultValue=-1L})){e->val d=e.arguments?.getString("date")?.let{runCatching{LocalDate.parse(it)}.getOrNull()};val id=e.arguments?.getLong("copy")?.takeIf{it>0};NewPlanScreen(viewModel{NewPlanViewModel(c,d,copyId=id)},{nav.popBackStack()})}
   composable("edit/{id}",arguments=listOf(navArgument("id"){type=NavType.LongType})){e->NewPlanScreen(viewModel{NewPlanViewModel(c,editId=e.arguments!!.getLong("id"))},{nav.popBackStack()},{id->nav.navigate("new?date=&copy=$id")})}
   composable("focus/{id}",arguments=listOf(navArgument("id"){type=NavType.LongType})){e->FocusScreen(viewModel{FocusViewModel(c,e.arguments!!.getLong("id"))},{nav.popBackStack()})}
  }
 }
 if(aiOpen)ModalBottomSheet(onDismissRequest={aiOpen=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=Background,contentColor=Primary){Box(Modifier.fillMaxHeight(.9f)){AiScreen(ai)}}
}
@Composable private fun SmallAiButton(onClick:()->Unit){Surface(onClick=onClick,shape=CircleShape,color=Surface,contentColor=Primary,shadowElevation=0.dp){Row(Modifier.padding(horizontal=18.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){Diamond();Spacer(Modifier.width(10.dp));Text("Rotam AI",style=MaterialTheme.typography.labelLarge)}}}
