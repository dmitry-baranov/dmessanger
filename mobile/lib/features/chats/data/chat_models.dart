class ChatSummary {
  const ChatSummary({
    required this.id,
    required this.type,
    required this.title,
    this.lastMessagePreview,
    this.lastEventAt,
    this.unreadCount = 0,
    this.isOnline = false,
  });

  final String id;
  final String type;
  final String title;
  final String? lastMessagePreview;
  final DateTime? lastEventAt;
  final int unreadCount;
  final bool isOnline;

  factory ChatSummary.fromJson(Map<String, dynamic> json) {
    final lastEventAt =
        json['lastEventAt'] ?? json['updatedAt'] ?? json['createdAt'];
    return ChatSummary(
      id: json['id'] as String,
      type: (json['type'] as String?) ?? 'DIRECT',
      title: (json['title'] as String?) ?? 'Без названия',
      lastMessagePreview: json['lastMessagePreview'] as String?,
      lastEventAt: lastEventAt is String
          ? DateTime.tryParse(lastEventAt)
          : null,
      unreadCount: (json['unreadCount'] as num?)?.toInt() ?? 0,
      isOnline: json['isOnline'] as bool? ?? false,
    );
  }
}
