# Step 2 — Canonical 42 Screens / Features Lock

Target branch: `azure-backend-additive`

This is the single canonical user-facing 42-screen list for the final Play Store repair cycle.
Existing feature files are preserved. No feature file is deleted or replaced by Step 2.

## Canonical 42
1. AzureHomeActivity — Real Azure Home / Profile
2. WelcomeActivity — Welcome / Language
3. HelpLineActivity — Help & Safety
4. ConversationHealthActivity — Conversation Health
5. NikahJourneyActivity — Nikah Journey
6. NikahBlueprintActivity — Nikah Blueprint
7. NikahMediatorActivity — AI Nikah Mediator
8. NikahSuccessPlanActivity — Nikah Success Plan
9. NikahSuccessNetworkActivity — Nikah Success Network
10. FutureLifeSimulationActivity — Future Life Simulation
11. SmartSeriousQuestionsActivity — Smart Serious Questions
12. MarriageTimelineMatchingActivity — Marriage Timeline Matching
13. NikahAssistantActivity — AI Nikah Assistant
14. CompatibilityDealBreakerActivity — Compatibility Deal-Breakers
15. SafeCommunicationActivity — Safe Communication
16. FamilyBridge2Activity — Family / Wali Connect
17. FamilyCircleActivity — Family Nikah Circle
18. TrustPassportActivity — Trust Passport
19. CompatibilityTrafficLightActivity — Compatibility Traffic Light
20. WhyWeMatchedActivity — Why We Matched
21. NikahIntelligenceActivity — Nikah Intelligence
22. SeriousNikahPlusActivity — Serious Nikah Plus
23. AdvancedMatchFiltersActivity — Advanced Match Filters
24. OwnerEarningsActivity — Owner Earnings / Payout
25. OwnerLiveAnalyticsActivity — Owner Live Analytics
26. WalletActivity — Azure Wallet / Ledger
27. PremiumPlansActivity — Premium 20 / 40 / 60
28. ProfilePhotoActivity — Profile Photo
29. RealFourPhotoActivity — Four-Photo Verification Flow
30. GenderFilteredMatchesActivity — Gender-Filtered Matches
31. PrivacyControlCenterActivity — Privacy Control Center
32. AccountDeletionActivity — Permanent Account Deletion
33. TermsAndCommunityGuidelinesActivity — Terms & Community Guidelines
34. CommunityChatActivity — Global Community Chat
35. BlockedMembersActivity — Blocked Members
36. IdentityVerificationActivity — Identity Verification
37. RewardedMessageActivity — Rewarded Message Credits
38. LikedMeActivity — Liked Me
39. ILikedActivity — I Liked
40. ViewedMeActivity — Viewed Me
41. IViewedActivity — I Viewed
42. ProductionMainActivity — Production Main Hub

## Infrastructure / admin / compatibility activities (not part of 42)
- AzureExternalAuthActivity — authentication infrastructure
- AzureWalletActivity — compatibility/alternate wallet screen; canonical wallet is WalletActivity
- HelpLineAdminActivity — privileged admin inbox
- VerificationAdminActivity — privileged admin review queue
- MainActivity — router/entry infrastructure
- ProductionCompletionActivity — completion/legacy helper
- PostVerificationFeatureHubActivity — duplicate/secondary feature hub
- UpgradeActivity — compatibility premium screen; canonical billing screen is PremiumPlansActivity

These 8 are NOT deleted by Step 2. Later cleanup may remove only items proven unused after dependency checks.

## Step 2 guarantees
- exactly 42 canonical activities
- every canonical Java file exists
- every canonical activity is registered in AndroidManifest.xml
- no canonical feature file is modified by this lock
- Firebase active runtime remains zero
- Azure remains the production target
