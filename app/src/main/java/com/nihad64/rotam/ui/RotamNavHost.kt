package com.nihad64.rotam.ui
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.nihad64.rotam.ui.plan.*
import com.nihad64.rotam.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
private data class Tab(val route:String,val label:String,val icon:ImageVector)
private val tabs=listOf(Tab("home","Bugün",Icons.Outlined.Home),Tab("calendar","Takvim",Icons.Outlined.CalendarMonth),Tab("routines","Rutinler",Icons.Outlined.Layers),Tab("settings","Ayarlar",Icons.Outlined.Settings))
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun RotamRoot(){
 val settings by RotamApp.container.settings.settings.collectAsStateWithLifecycle(initialValue=com.nihad64.rotam.data.prefs.Settings())
 CompositionLocalProvider(LocalRotamSettings provides settings){RotamContent()}
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun RotamContent(){
 val c=RotamApp.container;val ready by c.ready.collectAsStateWithLifecycle();val error by c.startupError.collectAsStateWithLifecycle();val scope=rememberCoroutineScope()
 if(!ready){Box(Modifier.fillMaxSize().background(Background).safeDrawingPadding().padding(28.dp),contentAlignment=Alignment.Center){if(error==null)CircularProgressIndicator()else Column(verticalArrangement=Arrangement.spacedBy(20.dp)){Text(error!!);PrimaryButton("Yeniden dene",{scope.launch{runCatching{c.initialize()}}})}};return}
 var welcome by remember{mutableStateOf<Boolean?>(null)}
 var welcomeStage by rememberSaveable{mutableIntStateOf(0)}
 var welcomeBusy by remember{mutableStateOf(false)}
 var welcomeError by remember{mutableStateOf<String?>(null)}
 var privacy by remember{mutableStateOf(false)}
 LaunchedEffect(Unit){welcome=!c.settings.current().welcomeCompleted&&c.db.planDao().all().isEmpty();if(welcome==true&&c.account.account.value!=null)welcomeStage=1}
 if(welcome==null){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()};return}
 if(welcome==true){
  fun finishWelcome(enable:Boolean){
   welcomeBusy=true;welcomeError=null
   scope.launch{try{if(enable)c.settings.setReminders(true);c.settings.completeWelcome();welcome=false
    try{c.scheduler.rescheduleAll()}catch(_:Exception){c.scheduler.reportFailure()}
   }catch(_:Exception){welcomeError="Tercihin kaydedilemedi. Yeniden deneyebilirsin."}finally{welcomeBusy=false}}
  }
  val requestNotifications=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->finishWelcome(granted)}
  val motionWelcome=rememberMotionEnabled()
  Box(Modifier.fillMaxSize().background(Background).safeDrawingPadding()){
   AnimatedContent(welcomeStage,transitionSpec={fadeIn(tween(if(motionWelcome)RotamMotion.Navigate else 0)) togetherWith fadeOut(tween(if(motionWelcome)RotamMotion.Exit else 0))},label="welcome-step"){step->
    if(step==0)com.nihad64.rotam.ui.welcome.WelcomeScreen(viewModel{SettingsViewModel(c)},onContinue={welcomeStage=1},onPrivacy={privacy=true})
    else com.nihad64.rotam.ui.welcome.ReminderWelcomeScreen(welcomeBusy,welcomeError,onEnable={
     if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(c.app,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){welcomeBusy=true;requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)}else finishWelcome(true)
    },onSkip={finishWelcome(false)})
   }
  }
  if(privacy)AlertDialog(onDismissRequest={privacy=false},title={Text("Verilerin senin kontrolünde")},text={Text("Planların cihazında saklanır. Rotam AI’ya gönderdiğin mesajlar yanıt üretmek için Google Gemini tarafından işlenir. Mevcut planlarını paylaşmak ayrıca senin tercihine bağlıdır. Cihazlar arası eşitleme, Verilerim bölümünden ayrıca onaylayarak açılabilir.")},confirmButton={TextButton(onClick={privacy=false}){Text("Anladım")}})
  return
 }
 val motion=rememberMotionEnabled()
 val nav=rememberNavController();val entry by nav.currentBackStackEntryAsState();val route=entry?.destination?.route
 var aiOpen by remember{mutableStateOf(false)};var draftDate by remember{mutableStateOf<LocalDate?>(null)};val ai:AiViewModel=viewModel{AiViewModel(c)}
 val snackbar=remember{SnackbarHostState()};val showBar=route in tabs.map{it.route}
 val planEditor=route.orEmpty().startsWith("new?")||route.orEmpty().startsWith("edit/")
 val focusManager=LocalFocusManager.current;val keyboard=LocalSoftwareKeyboardController.current
 LaunchedEffect(route){focusManager.clearFocus();keyboard?.hide()}
 val context=LocalContext.current
 LaunchedEffect(Unit){val id=(context as? android.app.Activity)?.intent?.getLongExtra("planId",-1)?:-1;if(id>0)nav.navigate("detail/$id")else if((context as? android.app.Activity)?.intent?.getBooleanExtra("newPlan",false)==true)nav.navigate("plan?date=")}
 LaunchedEffect(Unit){for(n in c.notices){val result=snackbar.showSnackbar(n.text,if(n.undo!=null)"Geri al"else null,withDismissAction=true,duration=SnackbarDuration.Long);if(result==SnackbarResult.ActionPerformed)runCatching{n.undo?.invoke()}}}
 fun manual(date:LocalDate?=null){nav.navigate("new?date=${date?:""}&copy=-1")}
 fun new(date:LocalDate?=null){nav.navigate("plan?date=${date?:""}")}
 Scaffold(containerColor=Background,contentWindowInsets=WindowInsets.safeDrawing,
  snackbarHost={SnackbarHost(snackbar,modifier=Modifier.padding(bottom=when{planEditor->160.dp;route.orEmpty().startsWith("plan?")->112.dp;else->0.dp})){data->Snackbar(data,containerColor=SurfaceVariant,contentColor=Primary,actionColor=Accent,dismissActionContentColor=SubtleText,shape=RoundedCornerShape(20.dp))}},bottomBar={if(showBar)Row(Modifier.fillMaxWidth().background(Background).navigationBarsPadding().padding(vertical=10.dp)){
   tabs.forEach{t->val selected=t.route==route;Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable{nav.navigate(t.route){popUpTo(nav.graph.findStartDestination().id){saveState=true};launchSingleTop=true;restoreState=true}}.padding(vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(t.icon,t.label,tint=if(selected)Accent else SubtleText,modifier=Modifier.size(25.dp));Text(t.label,style=MaterialTheme.typography.labelSmall,color=if(selected)Primary else SubtleText,modifier=Modifier.padding(top=5.dp));Spacer(Modifier.height(7.dp));Box(Modifier.size(4.dp).clip(CircleShape).background(if(selected)Accent else Background))}}
  }}){padding->
  NavHost(nav,startDestination="home",modifier=Modifier.padding(padding).consumeWindowInsets(padding),enterTransition={fadeIn(tween(if(motion)RotamMotion.Navigate else 0))+slideInHorizontally(tween(if(motion)RotamMotion.Navigate else 0)){it/14}},exitTransition={fadeOut(tween(if(motion)RotamMotion.Exit else 0))},popEnterTransition={fadeIn(tween(if(motion)RotamMotion.Replace else 0))},popExitTransition={fadeOut(tween(if(motion)RotamMotion.Exit else 0))+slideOutHorizontally(tween(if(motion)RotamMotion.Exit else 0)){it/14}}){
   composable("home"){HomeScreen(viewModel{HomeViewModel(c)},{new()},{nav.navigate("detail/$it")},{nav.navigate("focus/$it")},{nav.navigate("search")},{nav.navigate("stats")},{nav.navigate("settings")},onPostpone={nav.navigate("postpone/$it")})}
   composable("calendar"){val vm:CalendarViewModel=viewModel{CalendarViewModel(c)};CalendarScreen(vm,{new(vm.state.value.selected)},{nav.navigate("detail/$it")},onProgress={nav.navigate("stats")})}
   composable("routines"){com.nihad64.rotam.ui.routines.RoutinesScreen(c,onNew={new()},onEdit={nav.navigate("detail/$it")},onGoals={nav.navigate("goals")})}
   composable("plan?date={date}&edit={edit}",arguments=listOf(navArgument("date"){defaultValue=""},navArgument("edit"){type=NavType.LongType;defaultValue=-1L})){e->
    val d=e.arguments?.getString("date")?.let{runCatching{LocalDate.parse(it)}.getOrNull()}
    val edit=e.arguments?.getLong("edit")?.takeIf{it>0}
    LaunchedEffect(d,edit){ai.setPlanningDate(d);ai.setEditingPlan(edit)}
    AiScreen(ai,onManual={nav.popBackStack();manual(d)},onBack={nav.popBackStack()})
   }
   composable("goals"){GoalsScreen(viewModel{GoalsViewModel(c)},onBack={nav.navigate("home")})}
   composable("settings"){SettingsScreen(viewModel{SettingsViewModel(c)},onStats={nav.navigate("stats")},onSearch={nav.navigate("search")})}
   composable("stats"){StatsScreen(viewModel{StatsViewModel(c)},onBack={nav.popBackStack()},onPlan={nav.navigate("detail/$it")})}
   composable("search"){SearchScreen(c,{nav.popBackStack()},{nav.navigate("detail/$it")},onAsk={question->ai.setEditingPlan(null);ai.setPlanningDate(null);ai.setInput(question);nav.navigate("plan?date=");ai.send(question)})}
   composable("new?date={date}&copy={copy}",arguments=listOf(navArgument("date"){defaultValue=""},navArgument("copy"){type=NavType.LongType;defaultValue=-1L})){e->val d=e.arguments?.getString("date")?.let{runCatching{LocalDate.parse(it)}.getOrNull()};val id=e.arguments?.getLong("copy")?.takeIf{it>0};NewPlanScreen(viewModel{NewPlanViewModel(c,d,copyId=id)},{nav.popBackStack()},aiAction={SmallAiButton{aiOpen=true}})}
   composable("edit/{id}",arguments=listOf(navArgument("id"){type=NavType.LongType})){e->NewPlanScreen(viewModel{NewPlanViewModel(c,editId=e.arguments!!.getLong("id"))},{nav.popBackStack()},{id->nav.navigate("new?date=&copy=$id")},aiAction={SmallAiButton{aiOpen=true}})}
   composable("detail/{id}",arguments=listOf(navArgument("id"){type=NavType.LongType})){e->
    val id=e.arguments!!.getLong("id")
    PlanDetailScreen(viewModel{PlanDetailViewModel(c,id)},{nav.popBackStack()},{nav.navigate("edit/$id")},{nav.navigate("focus/$id")},{nav.navigate("postpone/$id")},{nav.navigate("plan?date=&edit=$id")})
   }
   composable("postpone/{id}",arguments=listOf(navArgument("id"){type=NavType.LongType})){e->PostponeScreen(viewModel{PlanDetailViewModel(c,e.arguments!!.getLong("id"))},{nav.popBackStack()})}
   composable("focus/{id}",arguments=listOf(navArgument("id"){type=NavType.LongType})){e->FocusScreen(viewModel{FocusViewModel(c,e.arguments!!.getLong("id"))},{nav.popBackStack()})}
  }
 }
 LaunchedEffect(aiOpen,draftDate){ai.setPlanningDate(draftDate)}
 if(aiOpen)ModalBottomSheet(onDismissRequest={aiOpen=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=Background,contentColor=Primary){Box(Modifier.fillMaxHeight(.9f)){AiScreen(ai,onManual={aiOpen=false;manual(draftDate)})}}
}
@Composable private fun SmallAiButton(onClick:()->Unit){Surface(onClick=onClick,shape=CircleShape,color=Surface,contentColor=Primary,shadowElevation=0.dp){Row(Modifier.padding(horizontal=18.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){Diamond();Spacer(Modifier.width(10.dp));Text("Rotam AI",style=MaterialTheme.typography.labelLarge)}}}
