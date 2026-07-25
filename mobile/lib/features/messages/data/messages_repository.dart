import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';

import '../../../core/http/api_client.dart';
import '../../auth/data/auth_controller.dart';
import 'message_models.dart';

final messagesRepositoryProvider = Provider<MessagesRepository>((ref) {
  return MessagesRepository(ref.watch(apiClientProvider));
});

final messagesProvider = FutureProvider.autoDispose
    .family<List<MessageItem>, String>((ref, chatId) async {
      final currentUserId = ref.watch(authControllerProvider).user?.id;
      return ref
          .watch(messagesRepositoryProvider)
          .fetchMessages(chatId, currentUserId: currentUserId);
    });

class MessagesRepository {
  MessagesRepository(this._api);

  final ApiClient _api;
  final _uuid = const Uuid();

  Future<List<MessageItem>> fetchMessages(
    String chatId, {
    String? currentUserId,
  }) async {
    final response = await _api.get<dynamic>('/api/v1/chats/$chatId/messages');
    final data = response.data;
    final list = switch (data) {
      final List<dynamic> raw => raw,
      final Map<String, dynamic> raw when raw['messages'] is List<dynamic> =>
        raw['messages'] as List<dynamic>,
      _ => <dynamic>[],
    };
    return list
        .whereType<Map<String, dynamic>>()
        .map((json) => MessageItem.fromJson(json, currentUserId: currentUserId))
        .toList(growable: false);
  }

  Future<void> sendDevTextMessage({
    required String chatId,
    required String text,
  }) async {
    final encoded = base64Encode(utf8.encode(text));
    await _api.post<Map<String, dynamic>>(
      '/api/v1/chats/$chatId/messages',
      data: {
        'clientMessageId': _uuid.v4(),
        'ciphertext': encoded,
        'encryptionMetadata': {
          'mode': 'dev-plaintext-base64',
          'warning': 'temporary MVP flow, not production E2EE',
        },
      },
    );
  }
}
