package com.nikahbridge;

import android.app.Activity;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/**
 * Isolated class-loading regression for EVERY activity.
 * It does not invoke onCreate, authenticate, charge, or call Azure.
 * Full click/navigation UI coverage remains PENDING.
 */
@RunWith(Parameterized.class)
public class All50ScreenLoadRobotTest {
  @Parameterized.Parameters(name="{index}: {0}")
  public static Collection<Object[]> screens() {
    String[] names = {
      "AccountDeletionActivity","AdvancedMatchFiltersActivity","AzureExternalAuthActivity","AzureHomeActivity",
      "AzureWalletActivity","BlockedMembersActivity","CommunityChatActivity","CompatibilityDealBreakerActivity",
      "CompatibilityTrafficLightActivity","ConversationHealthActivity","FamilyBridge2Activity","FamilyCircleActivity",
      "FutureLifeSimulationActivity","GenderFilteredMatchesActivity","HelpLineActivity","HelpLineAdminActivity",
      "ILikedActivity","IViewedActivity","IdentityVerificationActivity","LikedMeActivity","MainActivity",
      "MarriageTimelineMatchingActivity","NikahAssistantActivity","NikahBlueprintActivity","NikahIntelligenceActivity",
      "NikahJourneyActivity","NikahMediatorActivity","NikahSuccessNetworkActivity","NikahSuccessPlanActivity",
      "OwnerEarningsActivity","OwnerLiveAnalyticsActivity","PostVerificationFeatureHubActivity","PremiumPlansActivity",
      "PrivacyControlCenterActivity","ProductionCompletionActivity","ProductionMainActivity","ProfilePhotoActivity",
      "RealFourPhotoActivity","RewardedMessageActivity","SafeCommunicationActivity","SeriousNikahPlusActivity",
      "SmartSeriousQuestionsActivity","TermsAndCommunityGuidelinesActivity","TrustPassportActivity","UpgradeActivity",
      "VerificationAdminActivity","ViewedMeActivity","WalletActivity","WelcomeActivity","WhyWeMatchedActivity"
    };
    assertEquals("Must inventory exactly 50 screens",50,names.length);
    java.util.List<Object[]> rows=new java.util.ArrayList<>();
    for(String name:names)rows.add(new Object[]{name});
    return rows;
  }
  @Parameterized.Parameter public String name;
  @Test public void androidActivityClassLoadsSafely() throws Exception {
    Class<?> cl = Class.forName("com.nikahbridge."+name, false, getClass().getClassLoader());
    assertTrue(name+" is not an Android Activity", Activity.class.isAssignableFrom(cl));
  }
}
