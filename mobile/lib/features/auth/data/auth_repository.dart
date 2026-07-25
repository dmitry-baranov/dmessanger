import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/http/api_client.dart';
import '../../../core/storage/secure_token_storage.dart';
import 'user.dart';

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepository(ref.watch(apiClientProvider));
});

class AuthRepository {
  AuthRepository(this._api);

  final ApiClient _api;

  Future<AuthSession> login({
    required String login,
    required String password,
  }) async {
    final response = await _api.post<Map<String, dynamic>>(
      '/api/v1/auth/login',
      data: {'login': login, 'password': password},
    );
    return AuthSession.fromJson(response.data!);
  }

  Future<AuthSession> refresh(String refreshToken) async {
    final response = await _api.post<Map<String, dynamic>>(
      '/api/v1/auth/refresh',
      data: {'refreshToken': refreshToken},
    );
    return AuthSession.fromJson(response.data!);
  }

  Future<void> logout(String refreshToken) async {
    await _api.post<Map<String, dynamic>>(
      '/api/v1/auth/logout',
      data: {'refreshToken': refreshToken},
    );
  }

  Future<CurrentUser> me() async {
    final response = await _api.get<Map<String, dynamic>>('/api/v1/me');
    return CurrentUser.fromJson(response.data!);
  }
}

class AuthSession {
  const AuthSession({
    required this.tokens,
    required this.user,
    required this.expiresInSeconds,
  });

  final TokenPair tokens;
  final CurrentUser user;
  final int expiresInSeconds;

  factory AuthSession.fromJson(Map<String, dynamic> json) {
    return AuthSession(
      tokens: TokenPair(
        accessToken: json['accessToken'] as String,
        refreshToken: json['refreshToken'] as String,
      ),
      user: CurrentUser.fromJson(json['user'] as Map<String, dynamic>),
      expiresInSeconds: json['expiresInSeconds'] as int,
    );
  }
}
