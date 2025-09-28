package org.raibnowsea.cache.constant;

/**
 * starter自己的常量
 */
public class CacheAbleConstant {


    public static final Long DATA_SYNC_TTL = 200l;
    public static final String CACHE_REDIS_PROTOCOL = "redis://";
    public static final String CACHE_REDIS_PORT_SPLIT = ":";
    public static final String DISTRO_BLOOM_FILTER_NAME = "albumIdBloomFilter";
    public static final String DISTRO_BLOOM_FILTER_LOCK_KEY = "albumIdBloomFilter:lock";
    public static final String DISTRO_BLOOM_FILTER_LOCK_VALUE = "1";
    public static final Long DISTRO_BLOOM_FILTER_EXCEPTED_INSERT = 1000000l;
    public static final Double DISTRO_BLOOM_FILTER_FPP = 0.001;
    public static final Long  HAS_DATA_TTL =  60 * 60 * 24 * 7l;
    public static final Long  NO_DATA_TTL =  60 * 60 * 24l;

}
