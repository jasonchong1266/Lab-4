package com.jason.bigotraffic
import android.Manifest
import android.app.Activity
import android.content.*
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jason.bigotraffic.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
class MainActivity:AppCompatActivity(){private lateinit var b:ActivityMainBinding;private lateinit var db:TrafficDb
private val captureLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){r->if(r.resultCode==Activity.RESULT_OK&&r.data!=null){saveRoi();ContextCompat.startForegroundService(this,Intent(this,CaptureService::class.java).apply{putExtra("resultCode",r.resultCode);putExtra("data",r.data);putExtra("intervalMs",selectedInterval())});b.tvStatus.text="Status: Tracking — switch to BIGO"}else b.tvStatus.text="Status: Permission denied"}
private val notifLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
private val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context?,i:Intent?){val v=i?.getIntExtra(CaptureService.EXTRA_VIEWERS,-1)?:-1;val raw=i?.getStringExtra(CaptureService.EXTRA_RAW).orEmpty();b.tvLastRead.text="Last OCR: ${if(raw.isBlank())"(nothing detected)" else raw.replace("\n"," | ")}";if(v>0)b.tvCurrent.text="Current traffic: $v";refreshStats()}}
override fun onCreate(s:Bundle?){super.onCreate(s);b=ActivityMainBinding.inflate(layoutInflater);setContentView(b.root);db=TrafficDb(this);b.spInterval.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,listOf("10 seconds","30 seconds","60 seconds","2 minutes"));b.spInterval.setSelection(1);loadRoi();refreshStats();if(Build.VERSION.SDK_INT>=33)notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);b.btnStart.setOnClickListener{captureLauncher.launch((getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).createScreenCaptureIntent())};b.btnStop.setOnClickListener{startService(Intent(this,CaptureService::class.java).setAction(CaptureService.ACTION_STOP));b.tvStatus.text="Status: Stopped"};b.btnExport.setOnClickListener{exportCsv()}}
override fun onStart(){super.onStart();ContextCompat.registerReceiver(this,receiver,IntentFilter(CaptureService.ACTION_SAMPLE),ContextCompat.RECEIVER_NOT_EXPORTED)};override fun onStop(){unregisterReceiver(receiver);super.onStop()}
private fun selectedInterval()=when(b.spInterval.selectedItemPosition){0->10000L;1->30000L;2->60000L;else->120000L}
private fun saveRoi(){fun p(s:String,d:Float)=(s.toFloatOrNull()?.div(100f)?:d).coerceIn(0f,1f);getSharedPreferences("cfg",MODE_PRIVATE).edit().putFloat("roiL",p(b.etRoiLeft.text.toString(),.78f)).putFloat("roiT",p(b.etRoiTop.text.toString(),.04f)).putFloat("roiR",p(b.etRoiRight.text.toString(),.94f)).putFloat("roiB",p(b.etRoiBottom.text.toString(),.11f)).apply()}
private fun loadRoi(){val p=getSharedPreferences("cfg",MODE_PRIVATE);b.etRoiLeft.setText((p.getFloat("roiL",.78f)*100).toInt().toString());b.etRoiTop.setText((p.getFloat("roiT",.04f)*100).toInt().toString());b.etRoiRight.setText((p.getFloat("roiR",.94f)*100).toInt().toString());b.etRoiBottom.setText((p.getFloat("roiB",.11f)*100).toInt().toString())}
private fun refreshStats(){val(c,p,a)=db.stats();b.tvSamples.text="Samples: ${c?:0}";b.tvPeak.text="Peak traffic: ${p?:"—"}";b.tvAverage.text=if(a==null)"Average traffic: —" else "Average traffic: %.1f".format(a);db.latest()?.let{b.tvCurrent.text="Current traffic: ${it.viewers}"}}
private fun exportCsv(){val f=File(getExternalFilesDir(null),"bigo_traffic_${System.currentTimeMillis()}.csv");val fmt=SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault());f.bufferedWriter().use{w->w.appendLine("timestamp,viewers");db.all().forEach{w.appendLine("${fmt.format(Date(it.ts))},${it.viewers}")}};b.tvStatus.text="CSV saved: ${f.absolutePath}"}}
