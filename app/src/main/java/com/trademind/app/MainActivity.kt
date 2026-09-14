package com.trademind.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Bg = Color(0xFF0B1016)
private val Panel = Color(0xFF141C26)
private val Lime = Color(0xFFD7FF57)
private val Muted = Color(0xFF93A1B3)

enum class Screen { HOME, CHECK, COOLDOWN, JOURNAL, REPORT }
data class Entry(val symbol:String,val emotion:String,val reason:String,val action:String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TradeMind() }
    }
}

@Composable
fun TradeMind() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var symbol by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var emotion by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(listOf<Entry>()) }
    MaterialTheme(colorScheme=darkColorScheme(primary=Lime,background=Bg,surface=Panel)) {
        Scaffold(containerColor=Bg,bottomBar={
            if(screen!=Screen.COOLDOWN) NavigationBar(containerColor=Color(0xFF0D131A)) {
                listOf(Screen.HOME to "홈",Screen.JOURNAL to "기록",Screen.CHECK to "체크",Screen.REPORT to "리포트").forEach{(s,t)->
                    NavigationBarItem(selected=screen==s,onClick={screen=s},icon={Text(if(s==Screen.CHECK) "+" else "•")},label={Text(t)})
                }
            }
        }) { p ->
            when(screen){
                Screen.HOME->Home(p,{screen=Screen.CHECK},{screen=Screen.REPORT})
                Screen.CHECK->Check(p,symbol,{symbol=it},reason,{reason=it},emotion,{emotion=it}){if(symbol.isNotBlank()&&reason.isNotBlank()&&emotion.isNotBlank())screen=Screen.COOLDOWN}
                Screen.COOLDOWN->Cooldown(p,emotion in listOf("FOMO","불안","복수매매","흥분"),{
                    entries=listOf(Entry(symbol,emotion,reason,"계획대로 진행"))+entries;screen=Screen.JOURNAL
                },{
                    entries=listOf(Entry(symbol,emotion,reason,"매수 보류"))+entries;screen=Screen.JOURNAL
                })
                Screen.JOURNAL->Journal(p,entries)
                Screen.REPORT->Report(p)
            }
        }
    }
}

@Composable fun Page(p:PaddingValues,content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().padding(p).verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp),content=content)}
@Composable fun Header(k:String,t:String){Column{Text(k,color=Lime,fontSize=11.sp,fontWeight=FontWeight.Bold);Text(t,color=Color.White,fontSize=28.sp,fontWeight=FontWeight.Black)}}
@Composable fun BoxCard(content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(22.dp)).padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)}

@Composable fun Home(p:PaddingValues,onCheck:()->Unit,onReport:()->Unit)=Page(p){
    Header("TRADEMIND","오늘의 투자 상태")
    BoxCard{Text("오늘의 행동 점수",color=Muted);Row(verticalAlignment=Alignment.Bottom){Text("82",color=Lime,fontSize=58.sp,fontWeight=FontWeight.Black);Text("/100",color=Muted)};Text("충동 매매 위험은 낮습니다. 최근 급등주 추격 매수가 2회 감지됐습니다.",color=Color.White);TextButton(onClick=onReport){Text("분석 보기 →")}}
    Button(onClick=onCheck,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Lime,contentColor=Color.Black)){Text("매수 전 체크",fontWeight=FontWeight.Bold)}
    BoxCard{Text("오늘의 패턴",fontWeight=FontWeight.Bold);Text("시세 확인 3회 · 쿨다운 2회 · 규칙 준수 86%",color=Muted)}
    BoxCard{Text("주의 패턴",color=Color(0xFFFF7A7A),fontWeight=FontWeight.Bold);Text("상승률이 큰 종목을 장 초반에 따라붙는 경향",fontSize=20.sp,fontWeight=FontWeight.Bold);Text("당일 +8% 이상 급등 종목은 15분 쿨다운을 권장합니다.",color=Muted)}
}

@Composable fun Check(p:PaddingValues,symbol:String,onSymbol:(String)->Unit,reason:String,onReason:(String)->Unit,emotion:String,onEmotion:(String)->Unit,onStart:()->Unit)=Page(p){
    Header("PRE-TRADE CHECK","왜 지금 사려고 하나요?")
    OutlinedTextField(symbol,onSymbol,label={Text("종목명")},modifier=Modifier.fillMaxWidth())
    OutlinedTextField(reason,onReason,label={Text("매수 이유")},modifier=Modifier.fillMaxWidth())
    Text("현재 감정",color=Muted)
    listOf("차분함","확신","FOMO","불안","복수매매","흥분").chunked(3).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){row.forEach{e->FilterChip(selected=emotion==e,onClick={onEmotion(e)},label={Text(e)},modifier=Modifier.weight(1f))}}}
    Button(onClick=onStart,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Lime,contentColor=Color.Black)){Text("쿨다운 시작",fontWeight=FontWeight.Bold)}
}

@Composable fun Cooldown(p:PaddingValues,risky:Boolean,onProceed:()->Unit,onHold:()->Unit){var left by remember{mutableIntStateOf(if(risky)120 else 60)};LaunchedEffect(Unit){while(left>0){delay(1000);left--}}
    Page(p){Header("COOLDOWN","지금은 주문하지 마세요.");Text(if(risky)"감정 위험 신호가 있어 2분 쿨다운을 적용합니다." else "1분 뒤에도 같은 이유라면 다시 판단하세요.",color=Muted);Box(Modifier.size(220.dp).background(Panel,RoundedCornerShape(110.dp)).align(Alignment.CenterHorizontally),contentAlignment=Alignment.Center){Text("%02d:%02d".format(left/60,left%60),fontSize=46.sp,fontWeight=FontWeight.Black,color=Lime)};BoxCard{Text("지금 다시 확인하세요",fontWeight=FontWeight.Bold);Text("지금 사지 않으면 기회를 놓친다는 생각이 판단을 서두르게 하고 있지는 않나요?",color=Muted)};Button(onClick=onProceed,enabled=left==0,modifier=Modifier.fillMaxWidth()){Text("계획대로 진행")};OutlinedButton(onClick=onHold,modifier=Modifier.fillMaxWidth()){Text("오늘은 보류")}}
}

@Composable fun Journal(p:PaddingValues,entries:List<Entry>)=Page(p){Header("TRADE JOURNAL","매매일지");if(entries.isEmpty())BoxCard{Text("아직 기록이 없습니다.",color=Muted)}else entries.forEach{e->BoxCard{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(e.symbol,fontWeight=FontWeight.Bold);Text(e.action,color=if(e.action.contains("보류"))Color(0xFFFF7A7A)else Lime)};Text("감정: ${e.emotion}",color=Muted);Text(e.reason)}}}
@Composable fun Report(p:PaddingValues)=Page(p){Header("BEHAVIOR REPORT","이번 주 행동 리포트");BoxCard{Text("행동 점수",color=Muted);Text("82",color=Lime,fontSize=68.sp,fontWeight=FontWeight.Black);LinearProgressIndicator(progress={.82f},modifier=Modifier.fillMaxWidth());Text("매수 전 체크는 잘 지켰지만 장 초반 급등주 추격 매수가 반복됐습니다.",color=Muted)};BoxCard{Text("FOMO 38%",fontWeight=FontWeight.Bold);Text("시세 반복 확인 하루 평균 6.2회",color=Muted);Text("손절 기준 설정 91%",color=Lime)};BoxCard{Text("AI COACH",color=Lime,fontWeight=FontWeight.Bold);Text("다음 주 추천 행동",fontSize=20.sp,fontWeight=FontWeight.Bold);Text("당일 +8% 이상 급등 종목은 15분 쿨다운, 손실 직후에는 30분 신규 매수 제한을 권장합니다.",color=Muted)}}
