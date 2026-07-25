import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../core/http/api_exception.dart';
import '../../auth/data/auth_controller.dart';
import '../data/chat_models.dart';
import '../data/chats_repository.dart';

class ChatListScreen extends ConsumerWidget {
  const ChatListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final chats = ref.watch(chatsProvider);
    final auth = ref.watch(authControllerProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Чаты'),
        actions: [
          IconButton(
            tooltip: 'Обновить',
            onPressed: () => ref.invalidate(chatsProvider),
            icon: const Icon(Icons.refresh),
          ),
          IconButton(
            tooltip: 'Настройки',
            onPressed: () => context.push('/settings'),
            icon: const Icon(Icons.settings_outlined),
          ),
        ],
      ),
      body: chats.when(
        data: (items) => items.isEmpty
            ? _EmptyChats(userName: auth.user?.displayName)
            : RefreshIndicator(
                onRefresh: () async => ref.invalidate(chatsProvider),
                child: ListView.separated(
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  itemCount: items.length,
                  separatorBuilder: (_, _) =>
                      const Divider(height: 1, indent: 72),
                  itemBuilder: (context, index) =>
                      _ChatTile(chat: items[index]),
                ),
              ),
        error: (error, _) => _ChatsError(
          error: error,
          onRetry: () => ref.invalidate(chatsProvider),
        ),
        loading: () => const Center(child: CircularProgressIndicator()),
      ),
    );
  }
}

class _ChatTile extends StatelessWidget {
  const _ChatTile({required this.chat});

  final ChatSummary chat;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final time = chat.lastEventAt == null
        ? null
        : DateFormat.Hm().format(chat.lastEventAt!.toLocal());

    return ListTile(
      minVerticalPadding: 12,
      leading: Stack(
        clipBehavior: Clip.none,
        children: [
          CircleAvatar(
            radius: 24,
            child: Text(
              chat.title.characters.firstOrNull?.toUpperCase() ?? '?',
            ),
          ),
          if (chat.isOnline)
            Positioned(
              right: 0,
              bottom: 0,
              child: Container(
                width: 12,
                height: 12,
                decoration: BoxDecoration(
                  color: Colors.green,
                  shape: BoxShape.circle,
                  border: Border.all(
                    color: theme.colorScheme.surface,
                    width: 2,
                  ),
                ),
              ),
            ),
        ],
      ),
      title: Text(chat.title, maxLines: 1, overflow: TextOverflow.ellipsis),
      subtitle: Text(
        chat.lastMessagePreview ??
            (chat.type == 'GROUP' ? 'Групповой чат' : 'Личный чат'),
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      trailing: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          if (time != null)
            Text(
              time,
              style: theme.textTheme.labelSmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          if (chat.unreadCount > 0) ...[
            const SizedBox(height: 6),
            Badge(label: Text(chat.unreadCount.toString())),
          ],
        ],
      ),
      onTap: () => context.push(
        '/chats/${Uri.encodeComponent(chat.id)}?title=${Uri.encodeQueryComponent(chat.title)}',
      ),
    );
  }
}

class _EmptyChats extends StatelessWidget {
  const _EmptyChats({this.userName});

  final String? userName;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.forum_outlined,
              size: 48,
              color: theme.colorScheme.primary,
            ),
            const SizedBox(height: 16),
            Text(
              userName == null
                  ? 'Чаты пока пусты'
                  : '$userName, чаты пока пусты',
              textAlign: TextAlign.center,
              style: theme.textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            Text(
              'Frontend подключен к авторизации. Список чатов появится после реализации GET /api/v1/chats на backend.',
              textAlign: TextAlign.center,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ChatsError extends StatelessWidget {
  const _ChatsError({required this.error, required this.onRetry});

  final Object error;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final message = error is ApiException && (error as ApiException).isNotFound
        ? 'Backend API чатов ещё не реализован.'
        : error.toString();

    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.cloud_off_outlined,
              size: 48,
              color: theme.colorScheme.error,
            ),
            const SizedBox(height: 16),
            Text(
              message,
              textAlign: TextAlign.center,
              style: theme.textTheme.titleMedium,
            ),
            const SizedBox(height: 16),
            OutlinedButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('Повторить'),
            ),
          ],
        ),
      ),
    );
  }
}
