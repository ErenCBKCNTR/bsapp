import re

content = ""
with open("app/src/main/java/com/blind/social/ui/ChatScreen.kt", "r") as f:
    content = f.read()

import_statement = "import java.time.format.DateTimeFormatter\nimport java.time.Instant\nimport java.time.ZoneId\n"
content = content.replace("import java.io.File\n", "import java.io.File\n" + import_statement)
content = content.replace("import androidx.compose.material.icons.filled.PlayArrow", "import androidx.compose.material.icons.filled.PlayArrow\nimport androidx.compose.material.icons.filled.Check\nimport androidx.compose.ui.text.style.TextAlign\n")

# Let's find where the message bubble content is rendered
search_str = """                            } else {
                                Text(text = mesaj.metin, style = MaterialTheme.typography.bodyLarge)
                            }
                        }"""

replace_str = """                            } else {
                                Text(text = mesaj.metin, style = MaterialTheme.typography.bodyLarge)
                            }

                            val timeText = try {
                                mesaj.olusturmaTarihi?.let { dateStr ->
                                    val instant = Instant.parse(dateStr)
                                    val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                                    formatter.format(instant)
                                } ?: ""
                            } catch (e: Exception) {
                                ""
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = timeText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Okundu",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }"""

content = content.replace(search_str, replace_str)

with open("app/src/main/java/com/blind/social/ui/ChatScreen.kt", "w") as f:
    f.write(content)
