import re

content = ""
with open("app/src/main/java/com/blind/social/ui/ChatScreen.kt", "r") as f:
    content = f.read()

# Min API is 24, so java.time requires API 26 (desugaring is enabled but lint might complain, or we can just use SimpleDateFormat)

replace_str = """                            val timeText = try {
                                mesaj.olusturmaTarihi?.let { dateStr ->
                                    val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                                    parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                    val date = parser.parse(dateStr)
                                    val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                    formatter.timeZone = java.util.TimeZone.getDefault()
                                    formatter.format(date!!)
                                } ?: ""
                            } catch (e: Exception) {
                                ""
                            }"""

content = re.sub(r'val timeText = try \{.*?\} catch \(e: Exception\) \{\n\s*""\n\s*\}', replace_str, content, flags=re.DOTALL)

with open("app/src/main/java/com/blind/social/ui/ChatScreen.kt", "w") as f:
    f.write(content)
