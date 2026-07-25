import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/http/api_client.dart';
import 'chat_models.dart';

final chatsRepositoryProvider = Provider<ChatsRepository>((ref) {
  return ChatsRepository(ref.watch(apiClientProvider));
});

final chatsProvider = FutureProvider.autoDispose<List<ChatSummary>>((
  ref,
) async {
  return ref.watch(chatsRepositoryProvider).fetchChats();
});

class ChatsRepository {
  ChatsRepository(this._api);

  final ApiClient _api;

  Future<List<ChatSummary>> fetchChats() async {
    final response = await _api.get<dynamic>('/api/v1/chats');
    final data = response.data;
    final list = switch (data) {
      final List<dynamic> raw => raw,
      final Map<String, dynamic> raw when raw['chats'] is List<dynamic> =>
        raw['chats'] as List<dynamic>,
      _ => <dynamic>[],
    };
    return list
        .whereType<Map<String, dynamic>>()
        .map(ChatSummary.fromJson)
        .toList(growable: false);
  }
}
