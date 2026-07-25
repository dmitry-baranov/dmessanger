import 'dart:async';
import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

import '../config/app_config.dart';
import '../storage/secure_token_storage.dart';

final realtimeClientProvider = Provider<RealtimeClient>((ref) {
  return RealtimeClient(ref.watch(secureTokenStorageProvider));
});

class RealtimeClient {
  RealtimeClient(this._tokens);

  final SecureTokenStorage _tokens;
  WebSocketChannel? _channel;

  Stream<Map<String, dynamic>>? get events => _channel?.stream
      .where((event) => event is String)
      .map((event) => jsonDecode(event as String) as Map<String, dynamic>);

  Future<void> connect() async {
    final accessToken = await _tokens.readAccessToken();
    if (accessToken == null) {
      return;
    }

    final uri = Uri.parse(
      AppConfig.wsBaseUrl,
    ).replace(queryParameters: {'access_token': accessToken});
    _channel = WebSocketChannel.connect(uri);
  }

  Future<void> send(String type, Map<String, dynamic> payload) async {
    _channel?.sink.add(jsonEncode({'type': type, 'payload': payload}));
  }

  Future<void> close() async {
    await _channel?.sink.close();
    _channel = null;
  }
}
