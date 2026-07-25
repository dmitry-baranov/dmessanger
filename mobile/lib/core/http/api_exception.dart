import 'package:dio/dio.dart';

class ApiException implements Exception {
  const ApiException(this.message, {this.statusCode});

  final String message;
  final int? statusCode;

  bool get isNotFound => statusCode == 404;
  bool get isUnauthorized => statusCode == 401 || statusCode == 403;

  static ApiException fromDio(DioException error) {
    final response = error.response;
    final data = response?.data;
    String message = 'Не удалось выполнить запрос';

    if (data is Map<String, dynamic>) {
      final detail = data['message'] ?? data['error'] ?? data['detail'];
      if (detail is String && detail.isNotEmpty) {
        message = detail;
      }
    } else if (data is String && data.isNotEmpty) {
      message = data;
    } else if (error.message != null && error.message!.isNotEmpty) {
      message = error.message!;
    }

    return ApiException(message, statusCode: response?.statusCode);
  }

  @override
  String toString() => message;
}
