class AppConfig {
  static const apiBaseUrl = String.fromEnvironment(
    'DMESSANGER_API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080',
  );

  static const wsBaseUrl = String.fromEnvironment(
    'DMESSANGER_WS_BASE_URL',
    defaultValue: 'ws://10.0.2.2:8080/ws',
  );
}
