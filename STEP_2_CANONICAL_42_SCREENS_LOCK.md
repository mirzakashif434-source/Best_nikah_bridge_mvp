# Step 2 — Full 50 Azure Activities Lock

Target branch: `azure-backend-additive`

This is the single canonical 50-Activity inventory for the final Play Store repair cycle.
It includes the 42 user-facing feature screens plus 8 real auth/admin/helper/compatibility activities that participate in the Azure, navigation, wallet, billing, or privileged flows.

No existing feature activity is deleted or replaced by Step 2.

## Full 50 activities
1. AzureExternalAuthActivity — Azure / Microsoft Entra sign-in
2. AzureHomeActivity — Real Azure Home / Profile
3. AzureWalletActivity — Azure wallet compatibility flow
4. WelcomeActivity — Welcome / Language
5. HelpLineActivity — Help & Safety
6. HelpLineAdminActivity — Azure admin help inbox
7. ConversationHealthActivity — Conversation Health
8. NikahJourneyActivity — Nikah Journey
9. NikahBlueprintActivity — Nikah Blueprint
10. NikahMediatorActivity — AI Nikah Mediator
11. NikahSuccessPlanActivity — Nikah Success Plan
12. NikahSuccessNetworkActivity — Nikah Success Network
13. FutureLifeSimulationActivity — Future Life Simulation
14. SmartSeriousQuestionsActivity — Smart Serious Questions
15. MarriageTimelineMatchingActivity — Marriage Timeline Matching
16. NikahAssistantActivity — AI Nikah Assistant
17. CompatibilityDealBreakerActivity — Compatibility Deal-Breakers
18. SafeCommunicationActivity — Safe Communication
19. FamilyBridge2Activity — Family / Wali Connect
20. FamilyCircleActivity — Family Nikah Circle
21. TrustPassportActivity — Trust Passport
22. CompatibilityTrafficLightActivity — Compatibility Traffic Light
23. WhyWeMatchedActivity — Why We Matched
24. NikahIntelligenceActivity — Nikah Intelligence
25. SeriousNikahPlusActivity — Serious Nikah Plus
26. AdvancedMatchFiltersActivity — Advanced Match Filters
27. OwnerEarningsActivity — Owner Earnings / Payout
28. OwnerLiveAnalyticsActivity — Owner Live Analytics
29. WalletActivity — Azure Wallet / Ledger
30. PremiumPlansActivity — Premium 20 / 40 / 60
31. ProductionCompletionActivity — Production completion/helper flow
32. ProfilePhotoActivity — Profile Photo
33. RealFourPhotoActivity — Four-Photo Verification Flow
34. GenderFilteredMatchesActivity — Gender-Filtered Matches
35. PrivacyControlCenterActivity — Privacy Control Center
36. AccountDeletionActivity — Permanent Account Deletion
37. TermsAndCommunityGuidelinesActivity — Terms & Community Guidelines
38. CommunityChatActivity — Global Community Chat
39. BlockedMembersActivity — Blocked Members
40. IdentityVerificationActivity — Identity Verification
41. VerificationAdminActivity — Azure verification admin
42. RewardedMessageActivity — Rewarded Message Credits
43. LikedMeActivity — Liked Me
44. ILikedActivity — I Liked
45. ViewedMeActivity — Viewed Me
46. IViewedActivity — I Viewed
47. PostVerificationFeatureHubActivity — Verified feature hub
48. MainActivity — Azure auth/router entry
49. ProductionMainActivity — Production Main Hub
50. UpgradeActivity — Premium compatibility/upgrade flow

## Step 2 guarantees
- exactly 50 registered app activities are tracked
- every activity Java file exists
- every activity is registered in AndroidManifest.xml
- no existing activity file is modified by this lock
- Azure remains the production target
- Firebase active runtime remains zero
- auth/admin/helper/compatibility activities are protected together with the 42 feature screens
- later cleanup may remove a duplicate/helper activity only after dependency checks prove it is unused and safe to remove
