# Step 7 — Full 50 Activity Azure Wiring Map Lock

Target branch: `azure-backend-additive`

This file is the canonical production wiring map for all 50 app Activities.
A screen is classified by its real role: direct Azure API, Microsoft Entra auth, Google Play Billing,
AdMob/rewarded integration, or navigation/router into Azure-backed screens. No fake endpoint is assigned.

## 50/50 production mapping

1. AzureExternalAuthActivity — Microsoft Entra / Azure auth
2. AzureHomeActivity — /auth/azure/me, /profile, /matches, /interests, /family-links, /privacy, /verification, /safety/reports, /ai/nikah-assistant, /account
3. AzureWalletActivity — /wallet, /wallet/transactions
4. WelcomeActivity — Azure auth entry/router to AzureExternalAuthActivity
5. HelpLineActivity — /help/ask
6. HelpLineAdminActivity — /admin/help/tickets
7. ConversationHealthActivity — /conversations, /conversations/{id}/messages
8. NikahJourneyActivity — /journey/summary
9. NikahBlueprintActivity — /settings/nikah_blueprint, /compatibility/living
10. NikahMediatorActivity — /ai/nikah-mediator, /settings/nikah_mediator_last
11. NikahSuccessPlanActivity — /profile, /compatibility/living, /verification, /interests, /conversations
12. NikahSuccessNetworkActivity — /settings/nikah_success_network, /success-network/mentors, /success-network/mentor-requests
13. FutureLifeSimulationActivity — /settings/future_life_simulation, /ai/future-life
14. SmartSeriousQuestionsActivity — /matches, /matches/{id}
15. MarriageTimelineMatchingActivity — /matches
16. NikahAssistantActivity — /ai/nikah-assistant
17. CompatibilityDealBreakerActivity — /matches, /matches/{id}
18. SafeCommunicationActivity — /conversations, /conversations/{id}
19. FamilyBridge2Activity — /family-links, /family-links/pending-for-wali, /family-links/{id}
20. FamilyCircleActivity — /family-circle/join, /family-circle, /family-circle/invites, /family-circle/members, /matches, /family-circle/suggestions
21. TrustPassportActivity — /matches, /matches/{id}
22. CompatibilityTrafficLightActivity — /matches, /matches/{id}
23. WhyWeMatchedActivity — /matches
24. NikahIntelligenceActivity — /compatibility/living, /settings/nikah_intelligence
25. SeriousNikahPlusActivity — /premium/serious-plus-summary, /premium/who-liked-you
26. AdvancedMatchFiltersActivity — /premium/advanced-filters
27. OwnerEarningsActivity — /owner/earnings, /admin/owner/settlement-profile, /admin/owner/provider-settlement
28. OwnerLiveAnalyticsActivity — /owner/live-analytics
29. WalletActivity — /wallet, /wallet/withdrawals, /wallet/transactions
30. PremiumPlansActivity — Google Play Billing + /premium/plans, /premium/entitlement, /premium/purchases/verify
31. ProductionCompletionActivity — Azure-authenticated completion + AdMob/reward integration
32. ProfilePhotoActivity — Azure-authenticated photo flow / terms gate; routes into real photo verification
33. RealFourPhotoActivity — /profile, /photo-verification/start, /photo-verification/{id}, /photo-verification/status
34. GenderFilteredMatchesActivity — /presence/heartbeat, /matches, /likes, /family-circle/suggestions, /interests
35. PrivacyControlCenterActivity — /privacy
36. AccountDeletionActivity — DELETE /account
37. TermsAndCommunityGuidelinesActivity — /terms/accept
38. CommunityChatActivity — /community/mutes, /community/messages, /community/reports, /blocks
39. BlockedMembersActivity — /blocks
40. IdentityVerificationActivity — /verification
41. VerificationAdminActivity — /admin/verifications
42. RewardedMessageActivity — AdMob + /admob/rewarded/config
43. LikedMeActivity — /likes/received, /likes
44. ILikedActivity — /likes/sent, /likes/{id}
45. ViewedMeActivity — /profile-views/received, /likes
46. IViewedActivity — /profile-views/sent, /likes
47. PostVerificationFeatureHubActivity — navigation hub into Azure-backed Activities
48. MainActivity — Azure auth/router infrastructure
49. ProductionMainActivity — Azure-authenticated production hub + /owner/live-analytics + /admin/verifications
50. UpgradeActivity — navigation compatibility screen into real PremiumPlans / Google Play billing flow

## Step 7 guarantees
- all 50 Activities are explicitly mapped
- direct API screens keep real Azure routes
- auth screens keep Microsoft Entra wiring
- billing/ads screens keep their real platform integrations
- navigation-only screens are not given fake backend endpoints
- Firebase production runtime/config remains zero
- no existing Activity source is deleted or replaced by this lock
