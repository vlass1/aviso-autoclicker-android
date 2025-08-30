package top.dvptoken.avisoautoclicker

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.*
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var mainWeb: WebView
    private lateinit var ytWeb: WebView
    private lateinit var etMax: EditText
    private lateinit var etVMin: EditText
    private lateinit var etVMax: EditText
    private lateinit var etGMin: EditText
    private lateinit var etGMax: EditText
    private var running = false
    private val ui = Handler(Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etMax = findViewById(R.id.etMax)
        etVMin = findViewById(R.id.etVMin)
        etVMax = findViewById(R.id.etVMax)
        etGMin = findViewById(R.id.etGMin)
        etGMax = findViewById(R.id.etGMax)
        val btnStart: Button = findViewById(R.id.btnStart)
        val btnStop:  Button = findViewById(R.id.btnStop)

        mainWeb = findViewById(R.id.mainWeb)
        ytWeb   = findViewById(R.id.ytWeb)

        // AVISO WebView
        mainWeb.settings.javaScriptEnabled = true
        mainWeb.settings.domStorageEnabled = true
        mainWeb.settings.mediaPlaybackRequiresUserGesture = false
        mainWeb.settings.loadWithOverviewMode = true
        mainWeb.settings.useWideViewPort = true

        mainWeb.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                val transport = resultMsg?.obj as WebView.WebViewTransport
                transport.webView = ytWeb
                resultMsg.sendToTarget()
                showYt()
                return true
            }
        }
        mainWeb.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url?.contains("aviso.bz/tasks-youtube") == true && running) {
                    injectAvisoJs()
                }
            }
        }

        // YouTube WebView
        ytWeb.settings.javaScriptEnabled = true
        ytWeb.settings.domStorageEnabled = true
        ytWeb.settings.mediaPlaybackRequiresUserGesture = false
        ytWeb.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                ytWeb.evaluateJavascript("""
                    (function(){
                      function play(){
                        var v=document.querySelector('video');
                        if(v){ try{ v.muted=true; v.play(); }catch(e){} }
                        var b=document.querySelector('.ytp-large-play-button')
                              || document.querySelector("button[aria-label*='Play']")
                              || document.querySelector("button[aria-label*='Воспроизвести']");
                        if(b){ b.click(); }
                      }
                      setTimeout(play,800);
                    })();
                """.trimIndent(), null)
            }
        }

        btnStart.setOnClickListener {
            running = true
            mainWeb.loadUrl("https://aviso.bz/tasks-youtube")
            Toast.makeText(this, "Старт", Toast.LENGTH_SHORT).show()
        }
        btnStop.setOnClickListener {
            running = false
            Toast.makeText(this, "Стоп", Toast.LENGTH_SHORT).show()
        }

        mainWeb.loadUrl("https://aviso.bz/tasks-youtube")
    }

    private fun showYt() { ytWeb.visibility = WebView.VISIBLE }
    private fun hideYt() { ytWeb.visibility = WebView.GONE; ytWeb.loadUrl("about:blank") }

    private fun injectAvisoJs() {
        val maxTasks = (etMax.text.toString().toIntOrNull() ?: 30).coerceAtLeast(1)
        val vmin = etVMin.text.toString().toIntOrNull() ?: 12
        val vmax = max(vmin, etVMax.text.toString().toIntOrNull() ?: 16)
        val gmin = etGMin.text.toString().toIntOrNull() ?: 2
        val gmax = max(gmin, etGMax.text.toString().toIntOrNull() ?: 4)

        val js = """
          (function(){
            if (window.__avisoRun) return;
            window.__avisoRun = true;
            const sleep=ms=>new Promise(r=>setTimeout(r,ms));
            const rnd=(a,b)=>Math.floor(Math.random()*(b-a+1))+a;
            const TMAX=$maxTasks, VMIN=$vmin, VMAX=$vmax, GMIN=$gmin, GMAX=$gmax;

            function byText(tag,needle){
              const list=(tag==="*")?document.querySelectorAll("a,button,div,span"):document.querySelectorAll(tag);
              const n=(needle||"").toLowerCase();
              return Array.from(list).find(el=>(el.textContent||"").toLowerCase().includes(n));
            }
            function findYT(){
              const a1=Array.from(document.querySelectorAll("a[href]")).find(a=>/(youtube\.com|youtu\.be)/i.test(a.getAttribute("href")||""));
              if(a1) return a1.href;
              const idEl=Array.from(document.querySelectorAll("a,div,span")).find(el=>/^[A-Za-z0-9_-]{11}$/.test((el.textContent||"").trim()));
              if(idEl) return "https://youtu.be/"+(idEl.textContent||"").trim();
              return null;
            }
            async function waitTop(ms=15000){
              const t0=Date.now();
              while(Date.now()-t0<ms){
                if(byText("*","Осталось")||byText("*","Подтвердить просмотр")||byText("*","Отказаться от выполнения")) return true;
                await sleep(200);
              } return false;
            }
            async function waitConfirm(ms=35000){
              const t0=Date.now();
              while(Date.now()-t0<ms){
                const btn=Array.from(document.querySelectorAll("button,a")).find(b=>/Подтвердить просмотр|Проверить/i.test(b.textContent||""));
                if(btn && !(btn.disabled||btn.getAttribute("aria-disabled")==="true")) return btn;
                await sleep(300);
              } return null;
            }
            async function openRow(){
              let el=Array.from(document.querySelectorAll("a,button,div,span")).find(e=>/(посмотреть видео|просмотр видеоролика|video)/i.test(e.textContent||""));
              if(!el){ window.scrollBy(0,800); await sleep(500);
                el=Array.from(document.querySelectorAll("a,button,div,span")).find(e=>/(посмотреть видео|просмотр видеоролика|video)/i.test(e.textContent||"")); }
              if(!el) return false;
              (el.closest("a,button")||el).click(); await sleep(1000); return true;
            }
            async function loop(){
              for(let i=0;i<TMAX;i++){
                if(!(await openRow())){ window.scrollBy(0,800); await sleep(800); continue; }
                if(!(await waitTop())){ await sleep(800); continue; }
                const url=findYT(); if(!url){ await sleep(800); continue; }
                window.open(url,"_blank","noopener"); // перехватит второй WebView в приложении
                const ok=await waitConfirm(); if(ok){ ok.click(); await sleep(700); }
                const back=byText("a","К списку")||byText("a","Назад")||document.querySelector('a[href*="tasks-youtube"]');
                if(back){ back.click(); await sleep(1000); }
                await sleep(rnd(GMIN,GMAX)*1000);
              }
            }
            loop();
          })();
        """.trimIndent()

        mainWeb.evaluateJavascript(js, null)

        val watch = max(12, Random.nextInt(vmin, vmax + 1))
        ui.postDelayed({
            hideYt()
            val confirmJs = """
              (function(){
                var b=[...document.querySelectorAll('button,a')].find(x=>/Подтвердить просмотр|Проверить/i.test(x.textContent||''));
                if (b && !(b.disabled||b.getAttribute('aria-disabled')==='true')) b.click();
              })();
            """.trimIndent()
            mainWeb.evaluateJavascript(confirmJs, null)
        }, (watch * 1000).toLong())
    }
}
