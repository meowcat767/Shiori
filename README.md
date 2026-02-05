# Yomikomu - Manga Reader Desktop Application
[![Qodana](https://github.com/Yomikomu/Yomikomu/actions/workflows/qodana_code_quality.yml/badge.svg)](https://github.com/Yomikomu/Yomikomu/actions/workflows/qodana_code_quality.yml)[![CodeQL Advanced](https://github.com/Yomikomu/Yomikomu/actions/workflows/codeql.yml/badge.svg)](https://github.com/Yomikomu/Yomikomu/actions/workflows/codeql.yml)[![Dependabot Updates](https://github.com/Yomikomu/Yomikomu/actions/workflows/dependabot/dependabot-updates/badge.svg)](https://github.com/Yomikomu/Yomikomu/actions/workflows/dependabot/dependabot-updates)

A simple desktop manga reader application powered by the MangaDex API. Built with Java and Python, featuring a clean Swing-based UI with caching, bookmarks, and keyboard navigation.


## Features

- 🔍 **Manga Search** - Search and browse manga from MangaDex database
- 📖 **Chapter Reading** - Read chapters with page-by-page viewing
- 🔖 **Bookmarks** - Save and manage your reading progress
- 💾 **Image Caching** - Local caching for faster re-reading
- ⌨️ **Keyboard Navigation** - Full keyboard control for reading
- 🔍 **Zoom Controls** - Zoom in/out for better reading experience
- 📊 **Progress Tracking** - Loading progress and status indicators

## Requirements

- Java 21 or higher
- Maven 3.6+
- Internet connection (for MangaDex API access)

## Installation

### Build from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/Yomikomu.git
cd Yomikomu

# Build the project
mvn clean package

# Run the application
mvn exec:java
```

### Direct Run

```bash
mvn exec:java
```

## Usage

### Searching for Manga

1. Enter a title in the search box on the "Manga" tab
2. Press Enter to search
3. Click on a manga to view available chapters

### Reading Chapters

1. Select a manga from the search results
2. Choose a chapter from the "Chapters" tab
3. Use keyboard shortcuts or scroll to navigate

### Keyboard Shortcuts

| Key | Action |
|-----|--------|
| `N` / `→` | Next Chapter |
| `P` / `←` | Previous Chapter |
| `Space` | Page Down |
| `Shift + Space` | Page Up |
| `↓` | Scroll Down |
| `↑` | Scroll Up |
| `+` / `=` | Zoom In |
| `-` | Zoom Out |
| `0` | Reset Zoom |
| `Ctrl + Wheel` | Mouse Zoom |

### Bookmarks

- Click **Manga → Add Bookmark** to save your current reading position
- View bookmarks in the "Bookmarks" tab
- Bookmarks are stored in `~/.Yomikomu/bookmarks.json`

### Cache Management

- Images are cached in `~/.Yomikomu/cache/`
- Use **Advanced → Clear Cache** to clear cached images


## API Reference

### MangaDexClient

```java
// Search for manga by title
List<Manga> searchManga(String title) throws Exception

// Get chapters for a manga
List<Chapter> getChapters(String mangaId) throws Exception

// Get page URLs for a chapter
List<String> getPageUrls(String chapterId) throws Exception
```

### CacheManager

```java
// Check if URL is cached
boolean isCached(String url)

// Save data to cache
void saveToCache(String url, byte[] data) throws IOException

// Retrieve cached data
byte[] getFromCache(String url) throws IOException

// Clear all cached data
void clearCache()
```

## Configuration

Yomikomu stores configuration in the user's home directory:

- **Bookmarks**: `~/.Yomikomu/bookmarks.json`
- **Cache**: `~/.Yomikomu/cache/`

## Roadmap

See [TODO.md](TODO.md) for planned features and improvements.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

