package com.muljin.zendesk

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.internal.ContextUtils.getActivity
import com.zendesk.logger.Logger
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

// Zendesk
import zendesk.chat.Chat;
import zendesk.chat.VisitorInfo;
import zendesk.chat.ChatConfiguration;
import zendesk.chat.ChatMenuAction;
import zendesk.chat.ChatEngine;
import zendesk.configurations.Configuration
import zendesk.core.AnonymousIdentity.*
import zendesk.core.*
//import zendesk.core.Zendesk
import zendesk.messaging.MessagingActivity
import zendesk.support.Support
import zendesk.support.SupportEngine
import zendesk.support.guide.HelpCenterActivity
import zendesk.support.request.RequestActivity
import zendesk.support.requestlist.RequestListActivity
import zendesk.answerbot.AnswerBotEngine;
import zendesk.answerbot.AnswerBot;


/** ZendeskHelper */
class ZendeskHelper : FlutterPlugin, MethodCallHandler, ActivityAware {
  // / The MethodChannel that will the communication between Flutter and native Android
  // /
  // / This local reference serves to register the plugin with the Flutter Engine and unregister it
  // / when the Flutter Engine is detached from the Activity
  private lateinit var channel: MethodChannel
  private lateinit var activity: Activity
  private lateinit var context: Context

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "zendesk")
    channel.setMethodCallHandler(this)
    this.context = flutterPluginBinding.applicationContext
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onDetachedFromActivity() {
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
      activity = binding.activity
  }

  override fun onDetachedFromActivityForConfigChanges() {
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {

try {


    when (call.method) {
      "getPlatformVersion" -> {
          result.success("Android ${android.os.Build.VERSION.RELEASE}")
      }
      "initialize" -> {
        initialize(call)
        result.success(true)
      }
      "setVisitorInfo" -> {
        setVisitorInfo(call)
        result.success(true)
      }
      "startChat" -> {
        startChat(call)
        result.success(true)
      }
      "addTags" -> {
        addTags(call)
        result.success(true)
      }
      "removeTags" -> {
        removeTags(call)
        result.success(true)
      }
      else -> {
        result.notImplemented()
      }

    }
}catch (e:Exception){
  result.error("unKnowen",e.message,e.toString());
}
  }

  fun initialize(call: MethodCall) {
    Logger.setLoggable(BuildConfig.DEBUG)
    val accountKey = call.argument<String>("accountKey") ?: ""
    val applicationId = call.argument<String>("appId") ?: ""

    Zendesk.INSTANCE.init(this.context, "https://surfboardsupport.zendesk.com/", applicationId, "mobile_sdk_client_79d3bb251381bec3a125");
    Chat.INSTANCE.init(activity, accountKey, applicationId)
    Support.INSTANCE.init(Zendesk.INSTANCE)
    AnswerBot.INSTANCE.init(Zendesk.INSTANCE, Support.INSTANCE)


  }

  fun setVisitorInfo(call: MethodCall) {
    val name = call.argument<String>("name") ?: ""
    val email = call.argument<String>("email") ?: ""
    val phoneNumber = call.argument<String>("phoneNumber") ?: ""
    val department = call.argument<String>("department") ?: ""

    val profileProvider = Chat.INSTANCE.providers()?.profileProvider()
    val chatProvider = Chat.INSTANCE.providers()?.chatProvider()

    val visitorInfo = VisitorInfo.builder()
                                    .withName(name)
                                    .withEmail(email)
                                    .withPhoneNumber(phoneNumber) // numeric string
                                    .build()

    Zendesk.INSTANCE.setIdentity(
      AnonymousIdentity.Builder()
              .withNameIdentifier("{optional name}")
              .withEmailIdentifier("{optional email}")
              .build()
    );

    profileProvider?.setVisitorInfo(visitorInfo, null)
    chatProvider?.setDepartment(department, null)
  }

  fun addTags(call: MethodCall) {
    val tags = call.argument<List<String>>("tags") ?: listOf<String>()
    val profileProvider = Chat.INSTANCE.providers()?.profileProvider()
    profileProvider?.addVisitorTags(tags, null)
  }

  fun removeTags(call: MethodCall) {
    val tags = call.argument<List<String>>("tags") ?: listOf<String>()
    val profileProvider = Chat.INSTANCE.providers()?.profileProvider()
    profileProvider?.removeVisitorTags(tags, null)
  }

  fun startChat(call: MethodCall) {
    val isPreChatFormEnabled = call.argument<Boolean>("isPreChatFormEnabled") ?: true
    val isAgentAvailabilityEnabled = call.argument<Boolean>("isAgentAvailabilityEnabled") ?: true
    val isChatTranscriptPromptEnabled = call.argument<Boolean>("isChatTranscriptPromptEnabled") ?: true
    val isOfflineFormEnabled = call.argument<Boolean>("isOfflineFormEnabled") ?: true
    val chatConfigurationBuilder = ChatConfiguration.builder()
    chatConfigurationBuilder
        .withAgentAvailabilityEnabled(isAgentAvailabilityEnabled)
        .withTranscriptEnabled(isChatTranscriptPromptEnabled)
        .withOfflineFormEnabled(isOfflineFormEnabled)
        .withPreChatFormEnabled(isPreChatFormEnabled)
        .withChatMenuActions(ChatMenuAction.END_CHAT)

    val chatConfiguration = chatConfigurationBuilder.build()
try {
  MessagingActivity.builder()
          .withToolbarTitle("Contact Us")
          .withEngines(AnswerBotEngine.engine(), ChatEngine.engine(), SupportEngine.engine())
          .show(activity, chatConfiguration)
}catch ( e:Exception){
 throw e;
}

  }
}
