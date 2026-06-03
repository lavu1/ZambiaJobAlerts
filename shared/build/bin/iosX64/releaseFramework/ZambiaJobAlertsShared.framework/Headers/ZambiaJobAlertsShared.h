#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class ZJASJobTypeMapper, ZJASKotlinArray<T>, ZJASKotlinEnum<E>, ZJASKotlinEnumCompanion, ZJASSharedAdConfig, ZJASSharedApiConfig, ZJASSharedJobLaunchParser, ZJASSharedLaunchDestination, ZJASSharedLaunchRequest, ZJASSharedLaunchRequestCompanion, ZJASSharedLaunchRouter, ZJASSharedNotificationParser, ZJASSharedNotificationPayload, ZJASSharedServiceType;

@protocol ZJASKotlinComparable, ZJASKotlinIterator;

NS_ASSUME_NONNULL_BEGIN
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-warning-option"
#pragma clang diagnostic ignored "-Wincompatible-property-type"
#pragma clang diagnostic ignored "-Wnullability"

#pragma push_macro("_Nullable_result")
#if !__has_feature(nullability_nullable_result)
#undef _Nullable_result
#define _Nullable_result _Nullable
#endif

__attribute__((swift_name("KotlinBase")))
@interface ZJASBase : NSObject
- (instancetype)init __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (void)initialize __attribute__((objc_requires_super));
@end

@interface ZJASBase (ZJASBaseCopying) <NSCopying>
@end

__attribute__((swift_name("KotlinMutableSet")))
@interface ZJASMutableSet<ObjectType> : NSMutableSet<ObjectType>
@end

__attribute__((swift_name("KotlinMutableDictionary")))
@interface ZJASMutableDictionary<KeyType, ObjectType> : NSMutableDictionary<KeyType, ObjectType>
@end

@interface NSError (NSErrorZJASKotlinException)
@property (readonly) id _Nullable kotlinException;
@end

__attribute__((swift_name("KotlinNumber")))
@interface ZJASNumber : NSNumber
- (instancetype)initWithChar:(char)value __attribute__((unavailable));
- (instancetype)initWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
- (instancetype)initWithShort:(short)value __attribute__((unavailable));
- (instancetype)initWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
- (instancetype)initWithInt:(int)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
- (instancetype)initWithLong:(long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
- (instancetype)initWithLongLong:(long long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
- (instancetype)initWithFloat:(float)value __attribute__((unavailable));
- (instancetype)initWithDouble:(double)value __attribute__((unavailable));
- (instancetype)initWithBool:(BOOL)value __attribute__((unavailable));
- (instancetype)initWithInteger:(NSInteger)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
+ (instancetype)numberWithChar:(char)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
+ (instancetype)numberWithShort:(short)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
+ (instancetype)numberWithInt:(int)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
+ (instancetype)numberWithLong:(long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
+ (instancetype)numberWithLongLong:(long long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
+ (instancetype)numberWithFloat:(float)value __attribute__((unavailable));
+ (instancetype)numberWithDouble:(double)value __attribute__((unavailable));
+ (instancetype)numberWithBool:(BOOL)value __attribute__((unavailable));
+ (instancetype)numberWithInteger:(NSInteger)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
@end

__attribute__((swift_name("KotlinByte")))
@interface ZJASByte : ZJASNumber
- (instancetype)initWithChar:(char)value;
+ (instancetype)numberWithChar:(char)value;
@end

__attribute__((swift_name("KotlinUByte")))
@interface ZJASUByte : ZJASNumber
- (instancetype)initWithUnsignedChar:(unsigned char)value;
+ (instancetype)numberWithUnsignedChar:(unsigned char)value;
@end

__attribute__((swift_name("KotlinShort")))
@interface ZJASShort : ZJASNumber
- (instancetype)initWithShort:(short)value;
+ (instancetype)numberWithShort:(short)value;
@end

__attribute__((swift_name("KotlinUShort")))
@interface ZJASUShort : ZJASNumber
- (instancetype)initWithUnsignedShort:(unsigned short)value;
+ (instancetype)numberWithUnsignedShort:(unsigned short)value;
@end

__attribute__((swift_name("KotlinInt")))
@interface ZJASInt : ZJASNumber
- (instancetype)initWithInt:(int)value;
+ (instancetype)numberWithInt:(int)value;
@end

__attribute__((swift_name("KotlinUInt")))
@interface ZJASUInt : ZJASNumber
- (instancetype)initWithUnsignedInt:(unsigned int)value;
+ (instancetype)numberWithUnsignedInt:(unsigned int)value;
@end

__attribute__((swift_name("KotlinLong")))
@interface ZJASLong : ZJASNumber
- (instancetype)initWithLongLong:(long long)value;
+ (instancetype)numberWithLongLong:(long long)value;
@end

__attribute__((swift_name("KotlinULong")))
@interface ZJASULong : ZJASNumber
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value;
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value;
@end

__attribute__((swift_name("KotlinFloat")))
@interface ZJASFloat : ZJASNumber
- (instancetype)initWithFloat:(float)value;
+ (instancetype)numberWithFloat:(float)value;
@end

__attribute__((swift_name("KotlinDouble")))
@interface ZJASDouble : ZJASNumber
- (instancetype)initWithDouble:(double)value;
+ (instancetype)numberWithDouble:(double)value;
@end

__attribute__((swift_name("KotlinBoolean")))
@interface ZJASBoolean : ZJASNumber
- (instancetype)initWithBool:(BOOL)value;
+ (instancetype)numberWithBool:(BOOL)value;
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("JobTypeMapper")))
@interface ZJASJobTypeMapper : ZJASBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)jobTypeMapper __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) ZJASJobTypeMapper *shared __attribute__((swift_name("shared")));
- (ZJASInt * _Nullable)idForNameName:(NSString *)name __attribute__((swift_name("idForName(name:)")));
- (NSString *)nameForIdId:(int32_t)id __attribute__((swift_name("nameForId(id:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedAdConfig")))
@interface ZJASSharedAdConfig : ZJASBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)sharedAdConfig __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) ZJASSharedAdConfig *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *ANDROID_ADMOB_APP_ID __attribute__((swift_name("ANDROID_ADMOB_APP_ID")));
@property (readonly) NSString *ANDROID_APP_OPEN_AD_UNIT_ID __attribute__((swift_name("ANDROID_APP_OPEN_AD_UNIT_ID")));
@property (readonly) NSString *ANDROID_BANNER_PRIMARY_AD_UNIT_ID __attribute__((swift_name("ANDROID_BANNER_PRIMARY_AD_UNIT_ID")));
@property (readonly) NSString *ANDROID_BANNER_SECONDARY_AD_UNIT_ID __attribute__((swift_name("ANDROID_BANNER_SECONDARY_AD_UNIT_ID")));
@property (readonly) NSString *ANDROID_INTERSTITIAL_AD_UNIT_ID __attribute__((swift_name("ANDROID_INTERSTITIAL_AD_UNIT_ID")));
@property (readonly) NSString *ANDROID_NATIVE_AD_UNIT_ID __attribute__((swift_name("ANDROID_NATIVE_AD_UNIT_ID")));
@property (readonly) NSString *ANDROID_REWARDED_AD_UNIT_ID __attribute__((swift_name("ANDROID_REWARDED_AD_UNIT_ID")));
@property (readonly) NSString *IOS_ADMOB_APP_ID __attribute__((swift_name("IOS_ADMOB_APP_ID")));
@property (readonly) NSString *IOS_APP_OPEN_AD_UNIT_ID __attribute__((swift_name("IOS_APP_OPEN_AD_UNIT_ID")));
@property (readonly) NSString *IOS_BANNER_AD_UNIT_ID __attribute__((swift_name("IOS_BANNER_AD_UNIT_ID")));
@property (readonly) NSString *IOS_FIXED_BANNER_AD_UNIT_ID __attribute__((swift_name("IOS_FIXED_BANNER_AD_UNIT_ID")));
@property (readonly) NSString *IOS_INTERSTITIAL_AD_UNIT_ID __attribute__((swift_name("IOS_INTERSTITIAL_AD_UNIT_ID")));
@property (readonly) NSString *IOS_NATIVE_AD_UNIT_ID __attribute__((swift_name("IOS_NATIVE_AD_UNIT_ID")));
@property (readonly) NSString *IOS_NATIVE_VIDEO_AD_UNIT_ID __attribute__((swift_name("IOS_NATIVE_VIDEO_AD_UNIT_ID")));
@property (readonly) NSString *IOS_REWARDED_AD_UNIT_ID __attribute__((swift_name("IOS_REWARDED_AD_UNIT_ID")));
@property (readonly) NSString *IOS_REWARDED_INTERSTITIAL_AD_UNIT_ID __attribute__((swift_name("IOS_REWARDED_INTERSTITIAL_AD_UNIT_ID")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedApiConfig")))
@interface ZJASSharedApiConfig : ZJASBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)sharedApiConfig __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) ZJASSharedApiConfig *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *BASE_URL __attribute__((swift_name("BASE_URL")));
@property (readonly) NSString *LEGACY_GENERATE_TEXTS_URL __attribute__((swift_name("LEGACY_GENERATE_TEXTS_URL")));
@property (readonly) NSString *LEGACY_SERVICES_URL __attribute__((swift_name("LEGACY_SERVICES_URL")));
@property (readonly) NSString *PRIMARY_TEXT_MODEL_KEY __attribute__((swift_name("PRIMARY_TEXT_MODEL_KEY")));
@property (readonly) NSString *PRIMARY_TEXT_MODEL_NAME __attribute__((swift_name("PRIMARY_TEXT_MODEL_NAME")));
@property (readonly) NSString *PRIMARY_TEXT_MODEL_URL __attribute__((swift_name("PRIMARY_TEXT_MODEL_URL")));
@property (readonly) NSString *SECONDARY_TEXT_MODEL_KEY __attribute__((swift_name("SECONDARY_TEXT_MODEL_KEY")));
@property (readonly) NSString *SECONDARY_TEXT_MODEL_NAME __attribute__((swift_name("SECONDARY_TEXT_MODEL_NAME")));
@property (readonly) NSString *SECONDARY_TEXT_MODEL_URL __attribute__((swift_name("SECONDARY_TEXT_MODEL_URL")));
@property (readonly) NSString *TEXT_POLLINATIONS_ENDPOINT __attribute__((swift_name("TEXT_POLLINATIONS_ENDPOINT")));
@property (readonly) NSString *TEXT_POLLINATIONS_MODELS __attribute__((swift_name("TEXT_POLLINATIONS_MODELS")));
@property (readonly) NSString *WP_JOB_LISTINGS_URL __attribute__((swift_name("WP_JOB_LISTINGS_URL")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedJobLaunchParser")))
@interface ZJASSharedJobLaunchParser : ZJASBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)sharedJobLaunchParser __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) ZJASSharedJobLaunchParser *shared __attribute__((swift_name("shared")));
- (NSString *)buildJobDetailsBySlugUrlBaseUrl:(NSString *)baseUrl slug:(NSString *)slug __attribute__((swift_name("buildJobDetailsBySlugUrl(baseUrl:slug:)")));
- (NSString * _Nullable)extractIdentifierScheme:(NSString * _Nullable)scheme host:(NSString * _Nullable)host path:(NSString * _Nullable)path __attribute__((swift_name("extractIdentifier(scheme:host:path:)")));
- (BOOL)isHomeUriScheme:(NSString * _Nullable)scheme host:(NSString * _Nullable)host path:(NSString * _Nullable)path __attribute__((swift_name("isHomeUri(scheme:host:path:)")));
- (NSString * _Nullable)slugFromPathPath:(NSString * _Nullable)path __attribute__((swift_name("slugFromPath(path:)")));
@end

__attribute__((swift_name("KotlinComparable")))
@protocol ZJASKotlinComparable
@required
- (int32_t)compareToOther:(id _Nullable)other __attribute__((swift_name("compareTo(other:)")));
@end

__attribute__((swift_name("KotlinEnum")))
@interface ZJASKotlinEnum<E> : ZJASBase <ZJASKotlinComparable>
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) ZJASKotlinEnumCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(E)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) int32_t ordinal __attribute__((swift_name("ordinal")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedLaunchDestination")))
@interface ZJASSharedLaunchDestination : ZJASKotlinEnum<ZJASSharedLaunchDestination *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) ZJASSharedLaunchDestination *home __attribute__((swift_name("home")));
@property (class, readonly) ZJASSharedLaunchDestination *job __attribute__((swift_name("job")));
+ (ZJASKotlinArray<ZJASSharedLaunchDestination *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<ZJASSharedLaunchDestination *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedLaunchRequest")))
@interface ZJASSharedLaunchRequest : ZJASBase
- (instancetype)initWithDestination:(ZJASSharedLaunchDestination *)destination identifier:(NSString * _Nullable)identifier openedFromDeepLink:(BOOL)openedFromDeepLink __attribute__((swift_name("init(destination:identifier:openedFromDeepLink:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) ZJASSharedLaunchRequestCompanion *companion __attribute__((swift_name("companion")));
- (ZJASSharedLaunchRequest *)doCopyDestination:(ZJASSharedLaunchDestination *)destination identifier:(NSString * _Nullable)identifier openedFromDeepLink:(BOOL)openedFromDeepLink __attribute__((swift_name("doCopy(destination:identifier:openedFromDeepLink:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) ZJASSharedLaunchDestination *destination __attribute__((swift_name("destination")));
@property (readonly) NSString * _Nullable identifier __attribute__((swift_name("identifier")));
@property (readonly) BOOL openedFromDeepLink __attribute__((swift_name("openedFromDeepLink")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedLaunchRequest.Companion")))
@interface ZJASSharedLaunchRequestCompanion : ZJASBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) ZJASSharedLaunchRequestCompanion *shared __attribute__((swift_name("shared")));
- (ZJASSharedLaunchRequest *)forHome __attribute__((swift_name("forHome()")));
- (ZJASSharedLaunchRequest *)forJobIdentifier:(NSString *)identifier openedFromDeepLink:(BOOL)openedFromDeepLink __attribute__((swift_name("forJob(identifier:openedFromDeepLink:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedLaunchRouter")))
@interface ZJASSharedLaunchRouter : ZJASBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)sharedLaunchRouter __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) ZJASSharedLaunchRouter *shared __attribute__((swift_name("shared")));
- (NSString *)normalizeLaunchUrlValue:(NSString *)value __attribute__((swift_name("normalizeLaunchUrl(value:)")));
- (ZJASSharedLaunchRequest * _Nullable)parseNotificationLaunchJobId:(NSString * _Nullable)jobId jobSlug:(NSString * _Nullable)jobSlug deepLink:(NSString * _Nullable)deepLink __attribute__((swift_name("parseNotificationLaunch(jobId:jobSlug:deepLink:)")));
- (ZJASSharedLaunchRequest * _Nullable)parseUriScheme:(NSString * _Nullable)scheme host:(NSString * _Nullable)host path:(NSString * _Nullable)path openedFromDeepLink:(BOOL)openedFromDeepLink __attribute__((swift_name("parseUri(scheme:host:path:openedFromDeepLink:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedNotificationParser")))
@interface ZJASSharedNotificationParser : ZJASBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)sharedNotificationParser __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) ZJASSharedNotificationParser *shared __attribute__((swift_name("shared")));
- (ZJASSharedNotificationPayload *)fromMapData:(NSDictionary<NSString *, NSString *> *)data notificationTitle:(NSString * _Nullable)notificationTitle notificationBody:(NSString * _Nullable)notificationBody __attribute__((swift_name("fromMap(data:notificationTitle:notificationBody:)")));
- (ZJASSharedNotificationPayload *)fromValuesTitle:(NSString * _Nullable)title message:(NSString * _Nullable)message jobId:(NSString * _Nullable)jobId jobSlug:(NSString * _Nullable)jobSlug type:(NSString * _Nullable)type company:(NSString * _Nullable)company location:(NSString * _Nullable)location link:(NSString * _Nullable)link newWpPassword:(NSString * _Nullable)newWpPassword __attribute__((swift_name("fromValues(title:message:jobId:jobSlug:type:company:location:link:newWpPassword:)")));
- (ZJASSharedLaunchRequest *)launchRequestPayload:(ZJASSharedNotificationPayload *)payload __attribute__((swift_name("launchRequest(payload:)")));
- (BOOL)shouldSuppressVisibleNotificationPayload:(ZJASSharedNotificationPayload *)payload __attribute__((swift_name("shouldSuppressVisibleNotification(payload:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedNotificationPayload")))
@interface ZJASSharedNotificationPayload : ZJASBase
- (instancetype)initWithTitle:(NSString *)title message:(NSString *)message jobId:(NSString * _Nullable)jobId jobSlug:(NSString * _Nullable)jobSlug type:(NSString * _Nullable)type company:(NSString * _Nullable)company location:(NSString * _Nullable)location link:(NSString * _Nullable)link newWpPassword:(NSString * _Nullable)newWpPassword __attribute__((swift_name("init(title:message:jobId:jobSlug:type:company:location:link:newWpPassword:)"))) __attribute__((objc_designated_initializer));
- (ZJASSharedNotificationPayload *)doCopyTitle:(NSString *)title message:(NSString *)message jobId:(NSString * _Nullable)jobId jobSlug:(NSString * _Nullable)jobSlug type:(NSString * _Nullable)type company:(NSString * _Nullable)company location:(NSString * _Nullable)location link:(NSString * _Nullable)link newWpPassword:(NSString * _Nullable)newWpPassword __attribute__((swift_name("doCopy(title:message:jobId:jobSlug:type:company:location:link:newWpPassword:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable company __attribute__((swift_name("company")));
@property (readonly) NSString * _Nullable jobId __attribute__((swift_name("jobId")));
@property (readonly) NSString * _Nullable jobSlug __attribute__((swift_name("jobSlug")));
@property (readonly) NSString * _Nullable link __attribute__((swift_name("link")));
@property (readonly) NSString * _Nullable location __attribute__((swift_name("location")));
@property (readonly) NSString *message __attribute__((swift_name("message")));
@property (readonly, getter=doNewWpPassword) NSString * _Nullable newWpPassword __attribute__((swift_name("newWpPassword")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@property (readonly) NSString * _Nullable type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SharedServiceType")))
@interface ZJASSharedServiceType : ZJASKotlinEnum<ZJASSharedServiceType *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) ZJASSharedServiceType *emailAlerts __attribute__((swift_name("emailAlerts")));
@property (class, readonly) ZJASSharedServiceType *phoneAlerts __attribute__((swift_name("phoneAlerts")));
@property (class, readonly) ZJASSharedServiceType *priorityApplication __attribute__((swift_name("priorityApplication")));
@property (class, readonly) ZJASSharedServiceType *cvReview __attribute__((swift_name("cvReview")));
@property (class, readonly) ZJASSharedServiceType *cvWrite __attribute__((swift_name("cvWrite")));
@property (class, readonly) ZJASSharedServiceType *careerCoaching __attribute__((swift_name("careerCoaching")));
+ (ZJASKotlinArray<ZJASSharedServiceType *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<ZJASSharedServiceType *> *entries __attribute__((swift_name("entries")));
@property (readonly) int32_t creditCost __attribute__((swift_name("creditCost")));
@property (readonly) NSString *dayValue __attribute__((swift_name("dayValue")));
@property (readonly) NSString *requestType __attribute__((swift_name("requestType")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinEnumCompanion")))
@interface ZJASKotlinEnumCompanion : ZJASBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) ZJASKotlinEnumCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinArray")))
@interface ZJASKotlinArray<T> : ZJASBase
+ (instancetype)arrayWithSize:(int32_t)size init:(T _Nullable (^)(ZJASInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (T _Nullable)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (id<ZJASKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(T _Nullable)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((swift_name("KotlinIterator")))
@protocol ZJASKotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
