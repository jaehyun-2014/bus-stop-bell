import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:just_audio/just_audio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:android_intent_plus/android_intent.dart';
import 'dart:convert';
import 'dart:io';
import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';

void main() {
  runApp(const SoundboardApp());
}

class SoundboardApp extends StatelessWidget {
  const SoundboardApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '취향 사운드보드',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF7C3AED),
          brightness: Brightness.light,
        ),
        useMaterial3: true,
      ),
      home: const HomeScreen(),
    );
  }
}

class SoundCard {
  final String id;
  final String name;
  final String imagePath;
  final String soundPath;
  bool isWallpaper;

  SoundCard({
    required this.id,
    required this.name,
    required this.imagePath,
    required this.soundPath,
    this.isWallpaper = false,
  });

  Map<String, dynamic> toJson() => {
    'id': id,
    'name': name,
    'imagePath': imagePath,
    'soundPath': soundPath,
    'isWallpaper': isWallpaper,
  };

  factory SoundCard.fromJson(Map<String, dynamic> json) => SoundCard(
    id: json['id'],
    name: json['name'],
    imagePath: json['imagePath'],
    soundPath: json['soundPath'],
    isWallpaper: json['isWallpaper'] ?? false,
  );
}

class HomeScreen extends StatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  List<SoundCard> cards = [];
  late SharedPreferences prefs;
  late AudioPlayer audioPlayer;
  String? playingCardId;
  bool isEditMode = false;

  @override
  void initState() {
    super.initState();
    audioPlayer = AudioPlayer();
    _initializeApp();
  }

  Future<void> _initializeApp() async {
    prefs = await SharedPreferences.getInstance();
    await _loadCards();
  }

  Future<void> _loadCards() async {
    final jsonString = prefs.getString('soundCards');
    if (jsonString != null) {
      final jsonList = jsonDecode(jsonString) as List;
      setState(() {
        cards = jsonList.map((item) => SoundCard.fromJson(item)).toList();
      });
    }
  }

  Future<void> _saveCards() async {
    final jsonString = jsonEncode(cards.map((card) => card.toJson()).toList());
    await prefs.setString('soundCards', jsonString);
  }

  Future<void> _setWallpaper(SoundCard card) async {
    try {
      final imageFile = File(card.imagePath);
      if (!await imageFile.exists()) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('오류: 이미지 파일을 찾을 수 없습니다')),
        );
        return;
      }

      // UI 업데이트 (배경화면 표시 상태)
      setState(() {
        for (var c in cards) {
          c.isWallpaper = (c.id == card.id);
        }
      });
      await _saveCards();
      
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('배경화면으로 설정되었습니다!\n(My Files 앱에서 이미지를 길게 눌러 배경화면으로 설정하세요)')),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('오류가 발생했습니다')),
      );
    }
  }

  Future<void> _playSound(SoundCard card) async {
    try {
      setState(() => playingCardId = card.id);
      await audioPlayer.setFilePath(card.soundPath);
      await audioPlayer.play();
      
      Future.delayed(const Duration(seconds: 3), () {
        if (mounted) {
          setState(() => playingCardId = null);
        }
      });
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('오류: 음성을 재생할 수 없습니다')),
      );
      setState(() => playingCardId = null);
    }
  }

  Future<void> _addCard() async {
    String? imagePath;
    String? soundPath;
    String cardName = '';

    // 이미지 선택
    final imageResult = await ImagePicker().pickImage(
      source: ImageSource.gallery,
      imageQuality: 80,
    );
    if (imageResult == null) return;
    imagePath = imageResult.path;

    // 파일 앱으로 음성 파일 선택
    if (defaultTargetPlatform == TargetPlatform.android) {
      try {
        final AndroidIntent intent = AndroidIntent(
          action: 'android.intent.action.GET_CONTENT',
          type: 'audio/*',
        );
        final String? result = await intent.launchForResult();
        if (result == null || result.isEmpty) return;
        soundPath = result;
      } catch (e) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('오류: 파일 앱을 열 수 없습니다')),
        );
        return;
      }
    } else {
      final soundPathResult = await showDialog<String>(
        context: context,
        builder: (context) => _SoundPathInputDialog(),
      );
      if (soundPathResult == null || soundPathResult.isEmpty) return;
      soundPath = soundPathResult;
    }
    
    // 파일 존재 확인
    if (soundPath == null || !await File(soundPath).exists()) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('오류: 파일을 찾을 수 없습니다')),
      );
      return;
    }

    if (!mounted) return;

    // 카드 이름 입력
    final nameResult = await showDialog<String>(
      context: context,
      builder: (context) => _NameInputDialog(),
    );
    if (nameResult == null || nameResult.isEmpty) return;
    cardName = nameResult;

    final newCard = SoundCard(
      id: DateTime.now().toString(),
      name: cardName,
      imagePath: imagePath,
      soundPath: soundPath,
    );

    setState(() => cards.add(newCard));
    await _saveCards();

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('카드 추가 완료: $cardName')),
      );
    }
  }

  Future<void> _deleteCard(SoundCard card) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('삭제'),
        content: Text('${card.name} 카드를 삭제하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('삭제', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      setState(() => cards.removeWhere((c) => c.id == card.id));
      await _saveCards();
    }
  }

  @override
  void dispose() {
    audioPlayer.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('취향 사운드보드'),
        elevation: 0,
        actions: [
          Padding(
            padding: const EdgeInsets.all(8.0),
            child: Center(
              child: TextButton(
                onPressed: () => setState(() => isEditMode = !isEditMode),
                style: TextButton.styleFrom(
                  backgroundColor: isEditMode
                      ? Theme.of(context).colorScheme.primary
                      : Colors.transparent,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                    side: BorderSide(
                      color: Theme.of(context).colorScheme.outline,
                    ),
                  ),
                ),
                child: Text(
                  isEditMode ? '완료' : '편집',
                  style: TextStyle(
                    color: isEditMode
                        ? Colors.white
                        : Theme.of(context).colorScheme.onSurface,
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
      body: cards.isEmpty
          ? Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    Icons.music_note,
                    size: 64,
                    color: Theme.of(context).colorScheme.primary.withOpacity(0.5),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    '카드가 없습니다',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '아래 버튼을 눌러 첫 번째 카드를 추가하세요',
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                ],
              ),
            )
          : GridView.builder(
              padding: const EdgeInsets.all(16),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 2,
                mainAxisSpacing: 12,
                crossAxisSpacing: 12,
              ),
              itemCount: cards.length,
              itemBuilder: (context, index) {
                final card = cards[index];
                final isPlaying = playingCardId == card.id;

                return GestureDetector(
                  onTap: () => _playSound(card),
                  child: Container(
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: isPlaying
                            ? Theme.of(context).colorScheme.primary
                            : Theme.of(context).colorScheme.outline,
                        width: isPlaying ? 3 : 1,
                      ),
                    ),
                    child: Stack(
                      fit: StackFit.expand,
                      children: [
                        ClipRRect(
                          borderRadius: BorderRadius.circular(12),
                          child: Image.file(
                            File(card.imagePath),
                            fit: BoxFit.cover,
                          ),
                        ),
                        Container(
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(12),
                            gradient: LinearGradient(
                              begin: Alignment.topCenter,
                              end: Alignment.bottomCenter,
                              colors: [
                                Colors.transparent,
                                Colors.black.withOpacity(0.6),
                              ],
                            ),
                          ),
                        ),
                        Positioned(
                          bottom: 12,
                          left: 12,
                          right: 12,
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                card.name,
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 14,
                                  fontWeight: FontWeight.bold,
                                ),
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                              ),
                              if (isPlaying)
                                const Padding(
                                  padding: EdgeInsets.only(top: 4),
                                  child: Text(
                                    '재생 중...',
                                    style: TextStyle(
                                      color: Colors.amber,
                                      fontSize: 12,
                                    ),
                                  ),
                                ),
                            ],
                          ),
                        ),
                        // 배경화면 설정 버튼
                        Positioned(
                          top: 8,
                          left: 8,
                          child: GestureDetector(
                            onTap: () => _setWallpaper(card),
                            child: Container(
                              decoration: BoxDecoration(
                                color: card.isWallpaper ? Colors.green : Colors.blue.withOpacity(0.7),
                                borderRadius: BorderRadius.circular(20),
                              ),
                              padding: const EdgeInsets.all(6),
                              child: Icon(
                                card.isWallpaper ? Icons.check : Icons.wallpaper,
                                color: Colors.white,
                                size: 16,
                              ),
                            ),
                          ),
                        ),
                        if (isEditMode)
                          Positioned(
                            top: 8,
                            right: 8,
                            child: GestureDetector(
                              onTap: () => _deleteCard(card),
                              child: Container(
                                decoration: BoxDecoration(
                                  color: Colors.red,
                                  borderRadius: BorderRadius.circular(20),
                                ),
                                padding: const EdgeInsets.all(4),
                                child: const Icon(
                                  Icons.close,
                                  color: Colors.white,
                                  size: 16,
                                ),
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                );
              },
            ),
      floatingActionButton: FloatingActionButton(
        onPressed: _addCard,
        tooltip: '카드 추가',
        child: const Icon(Icons.add),
      ),
    );
  }
}

class _SoundPathInputDialog extends StatefulWidget {
  @override
  State<_SoundPathInputDialog> createState() => _SoundPathInputDialogState();
}

class _SoundPathInputDialogState extends State<_SoundPathInputDialog> {
  late TextEditingController controller;

  @override
  void initState() {
    super.initState();
    controller = TextEditingController();
  }

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('음성 파일 경로'),
      content: TextField(
        controller: controller,
        decoration: const InputDecoration(
          hintText: '예: /storage/emulated/0/Music/sound.mp3',
          border: OutlineInputBorder(),
        ),
        autofocus: true,
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('취소'),
        ),
        TextButton(
          onPressed: () => Navigator.pop(context, controller.text),
          child: const Text('확인'),
        ),
      ],
    );
  }
}

class _NameInputDialog extends StatefulWidget {
  @override
  State<_NameInputDialog> createState() => _NameInputDialogState();
}

class _NameInputDialogState extends State<_NameInputDialog> {
  late TextEditingController controller;

  @override
  void initState() {
    super.initState();
    controller = TextEditingController();
  }

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('카드 이름'),
      content: TextField(
        controller: controller,
        decoration: const InputDecoration(
          hintText: '예: 새소리',
          border: OutlineInputBorder(),
        ),
        autofocus: true,
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('취소'),
        ),
        TextButton(
          onPressed: () => Navigator.pop(context, controller.text),
          child: const Text('확인'),
        ),
      ],
    );
  }
}
