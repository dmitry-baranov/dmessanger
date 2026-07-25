class MessageItem {
  const MessageItem({
    required this.id,
    required this.chatId,
    required this.senderUserId,
    required this.body,
    required this.createdAt,
    this.status = 'SENT',
    this.isOwn = false,
  });

  final String id;
  final String chatId;
  final String senderUserId;
  final String body;
  final DateTime createdAt;
  final String status;
  final bool isOwn;

  factory MessageItem.fromJson(
    Map<String, dynamic> json, {
    String? currentUserId,
  }) {
    final senderUserId = json['senderUserId'] ?? json['sender_user_id'] ?? '';
    final body = json['plaintext'] ?? json['text'] ?? json['ciphertext'] ?? '';
    return MessageItem(
      id: json['id'] as String,
      chatId: (json['chatId'] ?? json['chat_id']) as String,
      senderUserId: senderUserId as String,
      body: body.toString(),
      createdAt:
          DateTime.tryParse(
            (json['createdAt'] ?? json['created_at']).toString(),
          ) ??
          DateTime.now(),
      status: (json['status'] as String?) ?? 'SENT',
      isOwn: currentUserId != null && senderUserId == currentUserId,
    );
  }
}
