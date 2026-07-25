import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/config/app_config.dart';
import '../../auth/data/auth_controller.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authControllerProvider);
    final user = auth.user;

    return Scaffold(
      appBar: AppBar(title: const Text('Настройки')),
      body: ListView(
        padding: const EdgeInsets.symmetric(vertical: 8),
        children: [
          if (user != null)
            ListTile(
              leading: const CircleAvatar(child: Icon(Icons.person_outline)),
              title: Text(user.displayName),
              subtitle: Text('@${user.login}'),
            ),
          const Divider(),
          const ListTile(
            leading: Icon(Icons.dns_outlined),
            title: Text('Backend'),
            subtitle: Text(AppConfig.apiBaseUrl),
          ),
          const ListTile(
            leading: Icon(Icons.sensors_outlined),
            title: Text('Realtime'),
            subtitle: Text(
              'Подготовлен клиент WebSocket; endpoint /ws ещё не реализован на backend',
            ),
          ),
          const ListTile(
            leading: Icon(Icons.key_outlined),
            title: Text('Ключи устройства'),
            subtitle: Text(
              'Secure storage подключен; E2EE device API будет следующим backend/frontend срезом',
            ),
          ),
          const Divider(),
          Padding(
            padding: const EdgeInsets.all(16),
            child: FilledButton.icon(
              onPressed: () async {
                await ref.read(authControllerProvider.notifier).logout();
                if (context.mounted) {
                  context.go('/');
                }
              },
              icon: const Icon(Icons.logout),
              label: const Text('Выйти'),
            ),
          ),
        ],
      ),
    );
  }
}
