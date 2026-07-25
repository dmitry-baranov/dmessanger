import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../chats/presentation/chat_list_screen.dart';
import '../data/auth_controller.dart';
import 'login_screen.dart';

class AuthGate extends ConsumerWidget {
  const AuthGate({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authControllerProvider);

    return switch (auth.status) {
      AuthStatus.authenticated => const ChatListScreen(),
      AuthStatus.unauthenticated => const LoginScreen(),
      AuthStatus.loading || AuthStatus.unknown => const _StartupScreen(),
    };
  }
}

class _StartupScreen extends StatelessWidget {
  const _StartupScreen();

  @override
  Widget build(BuildContext context) {
    return const Scaffold(body: Center(child: CircularProgressIndicator()));
  }
}
