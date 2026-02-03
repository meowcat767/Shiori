# TODO: Remove Discord Integration - COMPLETED

## Summary
All Discord Rich Presence functionality has been removed from the Shiori manga reader application.

## Files Modified

### 1. pom.xml ✓
- [x] Removed discord-game-sdk4j dependency
- [x] Removed gson dependency (used for Discord)
- [x] Removed discord-game-sdk4j and gson from maven-shade-plugin includes

### 2. src/main/java/ChapterListPanel.java ✓
- [x] Removed `onDiscordUpdate` field
- [x] Removed `onDiscordUpdate` from constructor parameter
- [x] Removed all `onDiscordUpdate` calls in list selection listener
- [x] Removed `onDiscordUpdate` calls in nextChapter() and previousChapter()

### 3. src/main/java/MainFrame.java ✓
- [x] Removed Discord import statements
- [x] Removed CLIENT_ID field
- [x] Removed discordCore field
- [x] Removed discordActivity field
- [x] Removed initDiscordPresence() method
- [x] Removed updateDiscordManga() method
- [x] Removed updateDiscordChapter() method
- [x] Removed setDiscordIdle() method
- [x] Removed Discord cleanup in windowClosing listener
- [x] Updated ChapterListPanel constructor call (removed onDiscordUpdate parameter)
- [x] Removed initDiscordPresence() call in constructor

### 4. .idea/discord.xml ✓
- [x] Deleted this IDE configuration file

## Next Steps
Run `mvn clean compile` to verify no build errors

