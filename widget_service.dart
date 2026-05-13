import 'dart:convert';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// 홈 화면 위젯과 Flutter 앱 간의 데이터 동기화를 담당하는 서비스 클래스입니다.
///
/// 사운드 타일이 추가/수정/삭제될 때마다 이 클래스를 통해
/// Android 네이티브 위젯에 변경 사항을 전달합니다.
class WidgetService {
  // Android 네이티브와 통신하는 MethodChannel
  // MainActivity.kt의 WIDGET_CHANNEL 상수와 동일한 값이어야 합니다.
  static const _channel = MethodChannel('com.manus.soundboard_flutter/widget');

  /// 사운드 타일 목록을 SharedPreferences에 JSON 형태로 저장합니다.
  ///
  /// Android 네이티브의 SoundWidgetConfigureActivity에서
  /// 이 데이터를 읽어 위젯 설정 화면에 표시합니다.
  ///
  /// [tiles]는 각 사운드 타일의 정보를 담은 Map 목록입니다.
  /// 각 Map은 다음 키를 포함해야 합니다:
  ///   - 'id': 고유 식별자 (String)
  ///   - 'name': 표시 이름 (String)
  ///   - 'imagePath': 이미지 파일의 절대 경로 (String)
  ///   - 'soundPath': 소리 파일의 절대 경로 (String)
  static Future<void> saveSoundTiles(List<Map<String, String>> tiles) async {
    final prefs = await SharedPreferences.getInstance();
    final jsonString = jsonEncode(tiles);
    // 'sound_tiles_json' 키로 저장합니다.
    // Android 네이티브에서는 'flutter.sound_tiles_json' 키로 접근합니다.
    await prefs.setString('sound_tiles_json', jsonString);
  }

  /// 홈 화면에 추가된 모든 사운드 위젯의 UI를 업데이트합니다.
  ///
  /// 사운드 타일이 수정되거나 삭제될 때 호출하여
  /// 위젯 화면에 최신 정보가 반영되도록 합니다.
  static Future<void> updateAllWidgets() async {
    try {
      await _channel.invokeMethod('updateWidgets');
    } on PlatformException catch (e) {
      // 위젯 업데이트 실패 시 무시 (앱 동작에 영향 없음)
      print('위젯 업데이트 실패: ${e.message}');
    }
  }

  /// 사운드 타일 저장 후 위젯을 업데이트합니다.
  /// 타일 추가/수정 시 이 메서드 하나만 호출하면 됩니다.
  static Future<void> syncTilesAndUpdateWidgets(
      List<Map<String, String>> tiles) async {
    await saveSoundTiles(tiles);
    await updateAllWidgets();
  }

  /// 현재 홈 화면에 추가된 위젯 ID 목록을 반환합니다.
  static Future<List<int>> getActiveWidgetIds() async {
    try {
      final result = await _channel.invokeMethod<List>('getActiveWidgetIds');
      return result?.cast<int>() ?? [];
    } on PlatformException {
      return [];
    }
  }
}
