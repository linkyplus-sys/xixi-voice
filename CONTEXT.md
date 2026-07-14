# 嘻嘻配音

## 项目概述
AI语音克隆/配音App

## 关键信息
- 路径：~/workspace/voice-clone-app/
- 技术栈：Kotlin + Compose + Hilt + Room + OkHttp
- TTS引擎：MiMo TTS API直连
- 已移除Kyant Backdrop

## UI规范
- BottomNav: fillMaxWidth(0.6f) + padding(bottom=40dp)
- 容器纯黑80%不透明 + 白色描边20%不透明
- 选中图标蓝色#2563EB / 未选中灰色
- APK通过飞书发送

## 待评估
- Qwen3-TTS（Apache2.0, 1.7B, 流式97ms, 3s克隆, RTX4080可跑）
- IndexTTS2（情绪好但音质不稳定需抽卡）

## 状态
已部署，考虑换TTS引擎
