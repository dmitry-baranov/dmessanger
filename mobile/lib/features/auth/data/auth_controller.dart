import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/http/api_exception.dart';
import '../../../core/storage/secure_token_storage.dart';
import 'auth_repository.dart';
import 'user.dart';

final authControllerProvider = NotifierProvider<AuthController, AuthState>(
  AuthController.new,
);

class AuthController extends Notifier<AuthState> {
  late final AuthRepository _authRepository;
  late final SecureTokenStorage _tokenStorage;

  @override
  AuthState build() {
    _authRepository = ref.read(authRepositoryProvider);
    _tokenStorage = ref.read(secureTokenStorageProvider);
    Future.microtask(restoreSession);
    return const AuthState.unknown();
  }

  Future<void> restoreSession() async {
    state = const AuthState.loading();
    final tokens = await _tokenStorage.read();
    if (tokens == null) {
      state = const AuthState.unauthenticated();
      return;
    }

    try {
      final session = await _authRepository.refresh(tokens.refreshToken);
      await _tokenStorage.save(session.tokens);
      state = AuthState.authenticated(session.user);
    } on Object {
      await _tokenStorage.clear();
      state = const AuthState.unauthenticated();
    }
  }

  Future<void> login({required String login, required String password}) async {
    state = const AuthState.loading();
    try {
      final session = await _authRepository.login(
        login: login,
        password: password,
      );
      await _tokenStorage.save(session.tokens);
      state = AuthState.authenticated(session.user);
    } on ApiException catch (error) {
      state = AuthState.unauthenticated(error.message);
    } on Object {
      state = const AuthState.unauthenticated('Не удалось войти');
    }
  }

  Future<void> logout() async {
    final refreshToken = await _tokenStorage.readRefreshToken();
    if (refreshToken != null) {
      try {
        await _authRepository.logout(refreshToken);
      } on Object {
        // Local logout must still clear credentials if the server is unavailable.
      }
    }
    await _tokenStorage.clear();
    state = const AuthState.unauthenticated();
  }
}

enum AuthStatus { unknown, loading, authenticated, unauthenticated }

class AuthState {
  const AuthState._({required this.status, this.user, this.errorMessage});

  const AuthState.unknown() : this._(status: AuthStatus.unknown);
  const AuthState.loading() : this._(status: AuthStatus.loading);
  const AuthState.authenticated(CurrentUser user)
    : this._(status: AuthStatus.authenticated, user: user);
  const AuthState.unauthenticated([String? errorMessage])
    : this._(status: AuthStatus.unauthenticated, errorMessage: errorMessage);

  final AuthStatus status;
  final CurrentUser? user;
  final String? errorMessage;
}
