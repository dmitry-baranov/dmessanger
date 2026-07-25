import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/auth/data/auth_repository.dart';

void main() {
  test('AuthSession parses backend auth response', () {
    final session = AuthSession.fromJson({
      'accessToken': 'access',
      'refreshToken': 'refresh',
      'expiresInSeconds': 900,
      'user': {
        'id': '6b32b8e5-9cb7-41f3-b855-1376c4e2db4f',
        'login': 'alice',
        'displayName': 'Alice',
      },
    });

    expect(session.tokens.accessToken, 'access');
    expect(session.tokens.refreshToken, 'refresh');
    expect(session.user.login, 'alice');
    expect(session.expiresInSeconds, 900);
  });
}
