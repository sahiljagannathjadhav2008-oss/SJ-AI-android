package com.sahiljadhav.sjai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val Bg = Color(0xFF070B12)
private val Panel = Color(0xFF101925)
private val Cyan = Color(0xFF00D9FF)
private val Muted = Color(0xFF91A4B7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SjAiScreen() }
    }

    @Composable
    private fun SjAiScreen() {
        var input by remember { mutableStateOf("") }
        var messages by remember { mutableStateOf(listOf(ChatMessage("SJ AI", "Hello! Main SJ AI hoon. Model initialize hone ke baad main offline jawab de sakta hoon."))) }
        var status by remember { mutableStateOf("Preparing bundled model…") }
        var busy by remember { mutableStateOf(false) }
        var ready by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                try {
                    val model = File(filesDir, "sjai-model.gguf")
                    if (!model.exists() || model.length() < 100_000_000L) {
                        assets.open("sjai-model.gguf").use { source -> model.outputStream().use { source.copyTo(it) } }
                    }
                    val result = NativeBridge.nativeLoadModel(model.absolutePath, 1024, 2)
                    withContext(Dispatchers.Main) { status = result; ready = result.startsWith("READY") }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { status = "Model error: ${e.message ?: "unknown"}" }
                }
            }
        }

        MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = Bg, surface = Panel)) {
            Column(Modifier.fillMaxSize().background(Bg).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("SJ AI", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("SMALL MODEL • LOCAL INTELLIGENCE", color = Cyan, fontSize = 10.sp, letterSpacing = 1.5.sp)
                    }
                    Surface(color = Color(0xFF102A34), shape = RoundedCornerShape(50)) {
                        Text("● OFFLINE", color = Cyan, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Surface(color = Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(status, color = if (ready) Cyan else Muted, modifier = Modifier.padding(12.dp), fontSize = 12.sp)
                }
                Spacer(Modifier.height(14.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(messages) { msg ->
                        val isUser = msg.role == "You"
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                            Text(msg.role, color = if (isUser) Cyan else Muted, fontSize = 11.sp)
                            Surface(color = if (isUser) Color(0xFF103342) else Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.widthIn(max = 320.dp)) {
                                Text(msg.text, color = Color.White, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f).background(Panel, RoundedCornerShape(14.dp)).padding(15.dp), textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp), decorationBox = { inner ->
                        if (input.isEmpty()) Text("Ask SJ AI…", color = Muted, fontSize = 14.sp)
                        inner()
                    })
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val question = input.trim()
                        if (question.isNotEmpty() && ready && !busy) {
                            input = ""; messages = messages + ChatMessage("You", question); busy = true
                            lifecycleScope.launch {
                                val answer = withContext(Dispatchers.IO) {
                                    val prompt = "<|im_start|>system\nYou are SJ AI, a helpful assistant. Answer clearly in the user's language.\n<|im_end|>\n" +
                                        messages.drop(1).takeLast(6).joinToString("\n") { m ->
                                            if (m.role == "You") "<|im_start|>user\n${m.text}<|im_end|>" else "<|im_start|>assistant\n${m.text}<|im_end|>"
                                        } + "\n<|im_start|>user\n$question<|im_end|>\n<|im_start|>assistant\n"
                                    try { NativeBridge.nativeGenerate(prompt, 180) } catch (e: Exception) { "Generation error: ${e.message}" }
                                }
                                messages = messages + ChatMessage("SJ AI", answer.trim().ifEmpty { "I couldn't generate a response." }); busy = false
                            }
                        }
                    }, enabled = ready && !busy && input.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF073746), contentColor = Cyan), shape = RoundedCornerShape(12.dp)) {
                        Text(if (busy) "…" else "SEND")
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text("Built by Sahil Jadhav • Phase 1 prototype", color = Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

data class ChatMessage(val role: String, val text: String)
