class CurrentUser {
  const CurrentUser({
    required this.id,
    required this.login,
    required this.displayName,
  });

  final String id;
  final String login;
  final String displayName;

  factory CurrentUser.fromJson(Map<String, dynamic> json) {
    return CurrentUser(
      id: json['id'] as String,
      login: json['login'] as String,
      displayName: json['displayName'] as String,
    );
  }
}
