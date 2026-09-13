package net.ccbluex.liquidbounce.utils.rotation;

import java.util.function.Function;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Complete collection of 200+ PRNG algorithms from batches 2, 3, and 4.
 * Custom PRNG interface used instead of java.util.random.RandomGenerator (Java 17+).
 */
public final class RngAlgorithms {

    private RngAlgorithms() {}

    /** Names of all available PRNG algorithms (inner classes implementing Prng) */
    public static final String[] ALGORITHM_NAMES = {
        "MD5Hash","SHA1Hash","SHA256Hash","SHA512Hash","SHA3_256Hash",
        "SHAKE128Hash","SHAKE256Hash","Blake2sHash","Blake2bHash","Blake3Hash",
        "RIPEMD160Hash","WhirlpoolHash","TigerHash","Skein256Hash","GroestlHash","JHHash",
        "SipHash24","Poly1305Hash","HighwayHash","KangarooTwelve",
        "RC4","RC4A","Spritz","A5_1","A5_2","Trivium","Grain128","Grain128a",
        "MICKEY128","SNOW3G","ZUC","Salsa20_8","Salsa20_12","ChaCha8","ChaCha12",
        "XSalsa20","XChaCha20","Sosemanuk","Dragon","NLS","Phelix","PolarBear",
        "Mir1","MUGI","Enocoro128","HC128b","HC256b","E0","Pike","Turing",
        "Leviathan","Fubuki","K2","WG7","Helix","Panama","WAKE","SEAL","SCREAM","Rabbit2",
        "AES128Ctr","DESCtr","TripleDESCtr","BlowfishCtr","TwofishCtr","SerpentCtr",
        "CamelliaCtr","SM4Ctr","ARIACtr","TEACtr","XTEACtr","XXTEACtr","RC5Ctr","RC6Ctr",
        "RC2Ctr","IDEACtr","CAST5Ctr","SEEDCtr","PRESENTCtr","CLEFIACtr","PiccoloCtr",
        "TWINECtr","HIGHTCtr","mCryptonCtr","SimonCtr","SpeckCtr","KASUMICtr","MISTY1Ctr",
        "SkipjackCtr","GOSTCtr",
        "NistCtrDrbg","NistHmacDrbg","NistHashDrbg","DualEcDrbg","AnsiX917","AnsiX931",
        "Fortuna2","Yarrow160","SP800_90A","MicaliSchnorr",
        "GaussianBoxMuller","GaussianZiggurat","GaussianRatioOfUniforms",
        "ExponentialS","PoissonKnuth","PoissonRejection","GammaMarsaglia",
        "BetaJohnk","BetaCheng","BinomialBTPE","BinomialInversion","GeometricS",
        "ParetoS","CauchyS","LogisticS","LaplaceS","WeibullS","TriangularS","GumbelS",
        "LogNormalS","StudentTS","ChiSquareS","FDistributionS","DirichletS","VonMisesS",
        "RayleighS","MaxwellS","WaldS","NakagamiS","ZipfS",
        "RandomizedQuickselect","RandomizedQuicksort","KargerMinCut","MillerRabin",
        "SolovayStrassen","SchwartzZippel","Freivalds","RabinKarp","PollardRho","PollardP1",
        "WalkSAT","GSAT","SimulatedAnnealing","RandomizedRounding","RandomizedGreedy",
        "RandomizedLoadBalancing","ReservoirSampling","ImportanceSampling","BootstrapSampling",
        "JackknifeSampling","SMOTESampling","ADASYNSampling","MCMCSampling","MetropolisHastings",
        "GibbsSampling","HamiltonianMC","SliceSampling","RejectionSampling","AdaptiveRejection",
        "SequentialMonteCarlo","BloomFilter","CountingBloomFilter","CuckooFilter","MinHash",
        "WeightedMinHash","BBitMinHash","SimHash","OnePermutationHashing","DensifiedMinHash",
        "SuperMinHash","HyperLogLog","HyperLogLogPP","KMV","ThetaSketch","CountMin","CountSketch",
        "AMSSketch","AchlioptasProjection","TDigestSampler","ZigZagSampler","BaggingSampler",
        "RandomPatchSampler","ExtraTreesSampler","AesCmacPrf","AesGmacPrf","AesPrf","AesXtsPrf",
        "HmacSha256Prf","HmacSha512Prf","HmacBlake2Prf","HmacSm3Prf","Kmac128","Kmac256"
    };

    static long splitmix64(long x) {
        long z = x + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
    static double u(long x) { return (x >>> 11) * 0x1.0p-53; }
    static double g(long x) {
        return Math.sqrt(-2 * Math.log(u(x) + 1e-300)) * Math.cos(6.283185307179586 * u(splitmix64(x)));
    }


    // =================================================
    // ==========  BATCH 2: Hash/PRF Algorithms  =======
    // =================================================

    /* 1 */ static final class MD5Hash implements Prng {
        private long ctr = 0, key;
        MD5Hash(){ this(0x1234L); } MD5Hash(long k){ key = k; }
        private static int f(int x,int y,int z,int r){ return (x & y) | (~x & z); }
        private static int g(int x,int y,int z,int r){ return (x & z) | (y & ~z); }
        private static int h(int x,int y,int z,int r){ return x ^ y ^ z; }
        private static int i(int x,int y,int z,int r){ return y ^ (x | ~z); }
        private static int rot(int x,int n){ return (x<<n)|(x>>>(32-n)); }
        private static final int[] K = {
                0xd76aa478,0xe8c7b756,0x242070db,0xc1bdceee,0xf57c0faf,0x4787c62a,0xa8304613,0xfd469501,
                0x698098d8,0x8b44f7af,0xffff5bb1,0x895cd7be,0x6b901122,0xfd987193,0xa679438e,0x49b40821,
                0xf61e2562,0xc040b340,0x265e5a51,0xe9b6c7aa,0xd62f105d,0x02441453,0xd8a1e681,0xe7d3fbc8,
                0x21e1cde6,0xc33707d6,0xf4d50d87,0x455a14ed,0xa9e3e905,0xfcefa3f8,0x676f02d9,0x8d2a4c8a,
                0xfffa3942,0x8771f681,0x6d9d6122,0xfde5380c,0xa4beea44,0x4bdecfa9,0xf6bb4b60,0xbebfbc70,
                0x289b7ec6,0xeaa127fa,0xd4ef3085,0x04881d05,0xd9d4d039,0xe6db99e5,0x1fa27cf8,0xc4ac5665,
                0xf4292244,0x432aff97,0xab9423a7,0xfc93a039,0x655b59c3,0x8f0ccc92,0xffeff47d,0x85845dd1,
                0x6fa87e4f,0xfe2ce6e0,0xa3014314,0x4e0811a1,0xf7537e82,0xbd3af235,0x2ad7d2bb,0xeb86d391};
        private static final int[] S = {
                7,12,17,22,7,12,17,22,7,12,17,22,7,12,17,22,
                5,9,14,20,5,9,14,20,5,9,14,20,5,9,14,20,
                4,11,16,23,4,11,16,23,4,11,16,23,4,11,16,23,
                6,10,15,21,6,10,15,21,6,10,15,21,6,10,15,21};
        private byte[] hash(byte[] input) {
            long bitLen = (long)input.length * 8;
            int padLen = ((input.length + 8) / 64 + 1) * 64;
            byte[] msg = new byte[padLen];
            System.arraycopy(input, 0, msg, 0, input.length);
            msg[input.length] = (byte)0x80;
            for(int i=0;i<8;i++) msg[padLen-8+i] = (byte)(bitLen >>> (8*i));
            int a0=0x67452301, b0=0xefcdab89, c0=0x98badcfe, d0=0x10325476;
            for(int off=0; off<padLen; off+=64){
                int[] M = new int[16];
                for(int j=0;j<16;j++) M[j] = (msg[off+j*4]&0xFF) | ((msg[off+j*4+1]&0xFF)<<8) | ((msg[off+j*4+2]&0xFF)<<16) | ((msg[off+j*4+3]&0xFF)<<24);
                int A=a0, B=b0, C=c0, D=d0;
                for(int k=0;k<64;k++){
                    int F, gg;
                    if(k<16){ F=f(B,C,D,k); gg=k; }
                    else if(k<32){ F=g(B,C,D,k); gg=(5*k+1)&15; }
                    else if(k<48){ F=h(B,C,D,k); gg=(3*k+5)&15; }
                    else { F=i(B,C,D,k); gg=(7*k)&15; }
                    F = F + A + K[k] + M[gg];
                    A = D; D = C; C = B;
                    B = B + rot(F, S[k]);
                }
                a0 += A; b0 += B; c0 += C; d0 += D;
            }
            byte[] out = new byte[16];
            int[] hh = {a0,b0,c0,d0};
            for(int j=0;j<4;j++) for(int b=0;b<4;b++) out[j*4+b] = (byte)(hh[j] >>> (8*b));
            return out;
        }
        public long nextLong(){
            byte[] in = new byte[8];
            long c = ctr++;
            for(int i=0;i<8;i++) in[i] = (byte)((key >>> (8*i)) ^ (c >>> (8*i)));
            byte[] out = hash(in);
            long r = 0;
            for(int i=0;i<8;i++) r |= ((long)(out[i]&0xFF)) << (8*i);
            return r;
        }
    }

    /* 2 */ static final class SHA1Hash implements Prng {
        private long ctr = 0, key; SHA1Hash(){ this(0x1234L); } SHA1Hash(long k){ key=k; }
        private byte[] hash(byte[] input){
            long bitLen = (long)input.length * 8;
            int padLen = ((input.length + 8) / 64 + 1) * 64;
            byte[] msg = new byte[padLen];
            System.arraycopy(input,0,msg,0,input.length);
            msg[input.length] = (byte)0x80;
            for(int i=0;i<8;i++) msg[padLen-1-i] = (byte)(bitLen >>> (8*i));
            int h0=0x67452301, h1=0xEFCDAB89, h2=0x98BADCFE, h3=0x10325476, h4=0xC3D2E1F0;
            for(int off=0; off<padLen; off+=64){
                int[] w = new int[80];
                for(int i=0;i<16;i++) w[i] = ((msg[off+i*4]&0xFF)<<24)|((msg[off+i*4+1]&0xFF)<<16)|((msg[off+i*4+2]&0xFF)<<8)|(msg[off+i*4+3]&0xFF);
                for(int i=16;i<80;i++) w[i] = Integer.rotateLeft(w[i-3]^w[i-8]^w[i-14]^w[i-16],1);
                int a=h0,b=h1,c=h2,d=h3,e=h4;
                for(int i=0;i<80;i++){
                    int f,k;
                    if(i<20){ f=(b&c)|(~b&d); k=0x5A827999; }
                    else if(i<40){ f=b^c^d; k=0x6ED9EBA1; }
                    else if(i<60){ f=(b&c)|(b&d)|(c&d); k=0x8F1BBCDC; }
                    else { f=b^c^d; k=0xCA62C1D6; }
                    int t = Integer.rotateLeft(a,5) + f + e + k + w[i];
                    e=d; d=c; c=Integer.rotateLeft(b,30); b=a; a=t;
                }
                h0+=a; h1+=b; h2+=c; h3+=d; h4+=e;
            }
            byte[] out = new byte[20];
            int[] hh = {h0,h1,h2,h3,h4};
            for(int j=0;j<5;j++) for(int b=0;b<4;b++) out[j*4+b] = (byte)(hh[j] >>> (8*(3-b)));
            return out;
        }
        public long nextLong(){
            byte[] in = new byte[8];
            long c = ctr++;
            for(int i=0;i<8;i++) in[i] = (byte)((key >>> (8*i)) ^ (c >>> (8*i)));
            byte[] out = hash(in);
            long r=0; for(int i=0;i<8;i++) r |= ((long)(out[i]&0xFF)) << (8*i);
            return r;
        }
    }

    /* 3 */ static final class SHA256Hash implements Prng {
        private long ctr = 0, key; SHA256Hash(){ this(0x1234L); } SHA256Hash(long k){ key=k; }
        private static final int[] K = {
                0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
                0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
                0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
                0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
                0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
                0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
                0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
                0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2};
        private byte[] hash(byte[] input){
            long bitLen = (long)input.length * 8;
            int padLen = ((input.length + 8) / 64 + 1) * 64;
            byte[] msg = new byte[padLen];
            System.arraycopy(input,0,msg,0,input.length);
            msg[input.length] = (byte)0x80;
            for(int i=0;i<8;i++) msg[padLen-1-i] = (byte)(bitLen >>> (8*i));
            int[] h = {0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,0x510e527f,0x9b05688c,0x1f83d9ab,0x5be0cd19};
            for(int off=0; off<padLen; off+=64){
                int[] w = new int[64];
                for(int i=0;i<16;i++) w[i] = ((msg[off+i*4]&0xFF)<<24)|((msg[off+i*4+1]&0xFF)<<16)|((msg[off+i*4+2]&0xFF)<<8)|(msg[off+i*4+3]&0xFF);
                for(int i=16;i<64;i++){
                    int s0 = Integer.rotateRight(w[i-15],7) ^ Integer.rotateRight(w[i-15],18) ^ (w[i-15]>>>3);
                    int s1 = Integer.rotateRight(w[i-2],17) ^ Integer.rotateRight(w[i-2],19) ^ (w[i-2]>>>10);
                    w[i] = w[i-16] + s0 + w[i-7] + s1;
                }
                int a=h[0],b=h[1],c=h[2],d=h[3],e=h[4],f=h[5],g=h[6],hh=h[7];
                for(int i=0;i<64;i++){
                    int S1 = Integer.rotateRight(e,6) ^ Integer.rotateRight(e,11) ^ Integer.rotateRight(e,25);
                    int ch = (e & f) ^ (~e & g);
                    int t1 = hh + S1 + ch + K[i] + w[i];
                    int S0 = Integer.rotateRight(a,2) ^ Integer.rotateRight(a,13) ^ Integer.rotateRight(a,22);
                    int maj = (a & b) ^ (a & c) ^ (b & c);
                    int t2 = S0 + maj;
                    hh=g; g=f; f=e; e=d+t1; d=c; c=b; b=a; a=t1+t2;
                }
                h[0]+=a; h[1]+=b; h[2]+=c; h[3]+=d; h[4]+=e; h[5]+=f; h[6]+=g; h[7]+=hh;
            }
            byte[] out = new byte[32];
            for(int j=0;j<8;j++) for(int b=0;b<4;b++) out[j*4+b] = (byte)(h[j] >>> (8*(3-b)));
            return out;
        }
        public long nextLong(){
            byte[] in = new byte[8];
            long c = ctr++;
            for(int i=0;i<8;i++) in[i] = (byte)((key >>> (8*i)) ^ (c >>> (8*i)));
            byte[] out = hash(in);
            long r=0; for(int i=0;i<8;i++) r |= ((long)(out[i]&0xFF)) << (8*i);
            return r;
        }
    }

    /* 4 */ static final class SHA512Hash implements Prng {
        private long ctr=0, key; SHA512Hash(){ this(0x1234L); } SHA512Hash(long k){ key=k; }
        private static final long[] K = {
                0x428a2f98d728ae22L,0x7137449123ef65cdL,0xb5c0fbcfec4d3b2fL,0xe9b5dba58189dbbcL,
                0x3956c25bf348b538L,0x59f111f1b605d019L,0x923f82a4af194f9bL,0xab1c5ed5da6d8118L,
                0xd807aa98a3030242L,0x12835b0145706fbeL,0x243185be4ee4b28cL,0x550c7dc3d5ffb4e2L,
                0x72be5d74f27b896fL,0x80deb1fe3b1696b1L,0x9bdc06a725c71235L,0xc19bf174cf692694L,
                0xe49b69c19ef14ad2L,0xefbe4786384f25e3L,0x0fc19dc68b8cd5b5L,0x240ca1cc77ac9c65L,
                0x2de92c6f592b0275L,0x4a7484aa6ea6e483L,0x5cb0a9dcbd41fbd4L,0x76f988da831153b5L,
                0x983e5152ee66dfabL,0xa831c66d2db43210L,0xb00327c898fb213fL,0xbf597fc7beef0ee4L,
                0xc6e00bf33da88fc2L,0xd5a79147930aa725L,0x06ca6351e003826fL,0x142929670a0e6e70L,
                0x27b70a8546d22ffcL,0x2e1b21385c26c926L,0x4d2c6dfc5ac42aedL,0x53380d139d95b3dfL,
                0x650a73548baf63deL,0x766a0abb3c77b2a8L,0x81c2c92e47edaee6L,0x92722c851482353bL,
                0xa2bfe8a14cf10364L,0xa81a664bbc423001L,0xc24b8b70d0f89791L,0xc76c51a30654be30L,
                0xd192e819d6ef5218L,0xd69906245565a910L,0xf40e35855771202aL,0x106aa07032bbd1b8L,
                0x19a4c116b8d2d0c8L,0x1e376c085141ab53L,0x2748774cdf8eeb99L,0x34b0bcb5e19b48a8L,
                0x391c0cb3c5c95a63L,0x4ed8aa4ae3418acbL,0x5b9cca4f7763e373L,0x682e6ff3d6b2b8a3L,
                0x748f82ee5defb2fcL,0x78a5636f43172f60L,0x84c87814a1f0ab72L,0x8cc702081a6439ecL,
                0x90befffa23631e28L,0xa4506cebde82bde9L,0xbef9a3f7b2c67915L,0xc67178f2e372532bL,
                0xca273eceea26619cL,0xd186b8c721c0c207L,0xeada7dd6cde0eb1eL,0xf57d4f7fee6ed178L,
                0x06f067aa72176fbaL,0x0a637dc5a2c898a6L,0x113f9804bef90daeL,0x1b710b35131c471bL,
                0x28db77f523047d84L,0x32caab7b40c72493L,0x3c9ebe0a15c9bebcL,0x431d67c49c100d4cL,
                0x4cc5d4becb3e42b6L,0x597f299cfc657e2aL,0x5fcb6fab3ad6faecL,0x6c44198c4a475817L};
        private byte[] hash(byte[] input){
            long bitLen = (long)input.length * 8;
            int padLen = ((input.length + 16) / 128 + 1) * 128;
            byte[] msg = new byte[padLen];
            System.arraycopy(input,0,msg,0,input.length);
            msg[input.length] = (byte)0x80;
            for(int i=0;i<8;i++) msg[padLen-1-i] = (byte)(bitLen >>> (8*i));
            long[] h = {0x6a09e667f3bcc908L,0xbb67ae8584caa73bL,0x3c6ef372fe94f82bL,0xa54ff53a5f1d36f1L,
                        0x510e527fade682d1L,0x9b05688c2b3e6c1fL,0x1f83d9abfb41bd6bL,0x5be0cd19137e2179L};
            for(int off=0; off<padLen; off+=128){
                long[] w = new long[80];
                for(int i=0;i<16;i++){
                    long v = 0;
                    for(int b=0;b<8;b++) v = (v<<8) | (msg[off+i*8+b]&0xFF);
                    w[i] = v;
                }
                for(int i=16;i<80;i++){
                    long s0 = Long.rotateRight(w[i-15],1) ^ Long.rotateRight(w[i-15],8) ^ (w[i-15]>>>7);
                    long s1 = Long.rotateRight(w[i-2],19) ^ Long.rotateRight(w[i-2],61) ^ (w[i-2]>>>6);
                    w[i] = w[i-16] + s0 + w[i-7] + s1;
                }
                long a=h[0],b=h[1],c=h[2],d=h[3],e=h[4],f=h[5],g=h[6],hh=h[7];
                for(int i=0;i<80;i++){
                    long S1 = Long.rotateRight(e,14) ^ Long.rotateRight(e,18) ^ Long.rotateRight(e,41);
                    long ch = (e & f) ^ (~e & g);
                    long t1 = hh + S1 + ch + K[i] + w[i];
                    long S0 = Long.rotateRight(a,28) ^ Long.rotateRight(a,34) ^ Long.rotateRight(a,39);
                    long maj = (a & b) ^ (a & c) ^ (b & c);
                    long t2 = S0 + maj;
                    hh=g; g=f; f=e; e=d+t1; d=c; c=b; b=a; a=t1+t2;
                }
                h[0]+=a; h[1]+=b; h[2]+=c; h[3]+=d; h[4]+=e; h[5]+=f; h[6]+=g; h[7]+=hh;
            }
            byte[] out = new byte[64];
            for(int j=0;j<8;j++) for(int b=0;b<8;b++) out[j*8+b] = (byte)(h[j] >>> (8*(7-b)));
            return out;
        }
        public long nextLong(){
            byte[] in = new byte[8];
            long c = ctr++;
            for(int i=0;i<8;i++) in[i] = (byte)((key >>> (8*i)) ^ (c >>> (8*i)));
            byte[] out = hash(in);
            long r=0; for(int i=0;i<8;i++) r |= ((long)(out[i]&0xFF)) << (8*i);
            return r;
        }
    }

    /* 5 */ static final class SHA3_256Hash implements Prng {
        private long ctr=0, key; SHA3_256Hash(){ this(0x1234L); } SHA3_256Hash(long k){ key=k; }
        private static final long[] RC = {
                0x0000000000000001L,0x0000000000008082L,0x800000000000808aL,0x8000000080008000L,
                0x000000000000808bL,0x0000000080000001L,0x8000000080008081L,0x8000000000008009L,
                0x000000000000008aL,0x0000000000000088L,0x0000000080008009L,0x000000008000000aL,
                0x000000008000808bL,0x800000000000008bL,0x8000000000008089L,0x8000000000008003L,
                0x8000000000008002L,0x8000000000000080L,0x000000000000800aL,0x800000008000000aL,
                0x8000000080008081L,0x8000000000008080L,0x0000000080000001L,0x8000000080008008L};
        private static final int[] R = {1,3,6,10,15,21,28,36,45,55,2,14,27,41,56,8,25,43,62,18,39,61,20,44};
        static void keccakF(long[] s){
            for(int round=0; round<24; round++){
                long[] C = new long[5];
                for(int x=0;x<5;x++) C[x] = s[x] ^ s[x+5] ^ s[x+10] ^ s[x+15] ^ s[x+20];
                long[] D = new long[5];
                for(int x=0;x<5;x++) D[x] = C[(x+4)%5] ^ Long.rotateLeft(C[(x+1)%5],1);
                for(int x=0;x<5;x++) for(int y=0;y<5;y++) s[x+y*5] ^= D[x];
                long[] B = new long[25];
                for(int x=0;x<5;x++) for(int y=0;y<5;y++){
                    int nx = y, ny = (2*x+3*y)%5;
                    B[nx + ny*5] = Long.rotateLeft(s[x + y*5], R[(x + 5*y)%23]);
                }
                for(int x=0;x<5;x++) for(int y=0;y<5;y++){
                    s[x+y*5] = B[x+y*5] ^ (~B[(x+1)%5 + y*5] & B[(x+2)%5 + y*5]);
                }
                s[0] ^= RC[round];
            }
        }
        byte[] hash(byte[] input, int outLen){
            int rate = 136;
            long[] s = new long[25];
            int pos = 0;
            while(pos < input.length){
                int blk = Math.min(rate, input.length - pos);
                for(int i=0;i<blk;i++) s[i/8] ^= ((long)(input[pos+i]&0xFF)) << (8*(i%8));
                if(blk == rate){ keccakF(s); }
                pos += blk;
            }
            int rem = input.length % rate;
            int absorbPos = rem;
            s[absorbPos/8] ^= ((long)0x06) << (8*(absorbPos%8));
            s[(rate-1)/8] ^= ((long)0x80) << (8*((rate-1)%8));
            keccakF(s);
            byte[] out = new byte[outLen];
            for(int i=0;i<outLen;i++) out[i] = (byte)(s[i/8] >>> (8*(i%8)));
            return out;
        }
        public long nextLong(){
            byte[] in = new byte[8];
            long c = ctr++;
            for(int i=0;i<8;i++) in[i] = (byte)((key >>> (8*i)) ^ (c >>> (8*i)));
            byte[] out = hash(in, 32);
            long r=0; for(int i=0;i<8;i++) r |= ((long)(out[i]&0xFF)) << (8*i);
            return r;
        }
    }

    /* 6 */ static final class SHAKE128Hash implements Prng {
        private long ctr=0, key; SHAKE128Hash(){ this(0x1234L); } SHAKE128Hash(long k){ key=k; }
        public long nextLong(){
            byte[] in = new byte[8];
            long c = ctr++;
            for(int i=0;i<8;i++) in[i] = (byte)((key >>> (8*i)) ^ (c >>> (8*i)));
            int rate = 168;
            long[] s = new long[25];
            int absorbPos = 0;
            for(int i=0;i<in.length;i++){
                s[absorbPos/8] ^= ((long)(in[i]&0xFF)) << (8*(absorbPos%8));
                absorbPos++;
            }
            s[absorbPos/8] ^= ((long)0x1F) << (8*(absorbPos%8));
            s[(rate-1)/8] ^= ((long)0x80) << (8*((rate-1)%8));
            SHA3_256Hash.keccakF(s);
            long r = 0;
            for(int i=0;i<8;i++) r |= ((s[i/8] >>> (8*(i%8))) & 0xFF) << (8*i);
            return r;
        }
    }

    /* 7 */ static final class SHAKE256Hash implements Prng {
        private long ctr=0, key; SHAKE256Hash(){ this(0x1234L); } SHAKE256Hash(long k){ key=k; }
        public long nextLong(){
            byte[] in = new byte[8];
            long c = ctr++;
            for(int i=0;i<8;i++) in[i] = (byte)((key >>> (8*i)) ^ (c >>> (8*i)));
            int rate = 136;
            long[] s = new long[25];
            int absorbPos = 0;
            for(int i=0;i<in.length;i++){
                s[absorbPos/8] ^= ((long)(in[i]&0xFF)) << (8*(absorbPos%8));
                absorbPos++;
            }
            s[absorbPos/8] ^= ((long)0x1F) << (8*(absorbPos%8));
            s[(rate-1)/8] ^= ((long)0x80) << (8*((rate-1)%8));
            SHA3_256Hash.keccakF(s);
            long r = 0;
            for(int i=0;i<8;i++) r |= ((s[i/8] >>> (8*(i%8))) & 0xFF) << (8*i);
            return r;
        }
    }

    /* 8 */ static final class Blake2sHash implements Prng {
        private long ctr=0, key; Blake2sHash(){ this(0x1234L); } Blake2sHash(long k){ key=k; }
        private static final int[] IV = {0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,0x510e527f,0x9b05688c,0x1f83d9ab,0x5be0cd19};
        private static final byte[][] SIGMA = {
                {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15},
                {14,10,4,8,9,15,13,6,1,12,0,2,11,7,5,3},
                {11,8,12,0,5,2,15,13,10,14,3,6,7,1,9,4},
                {7,9,3,1,13,12,11,14,2,6,5,10,4,0,15,8},
                {9,0,5,7,2,4,10,15,14,1,11,12,6,8,3,13},
                {2,12,6,10,0,11,8,3,4,13,7,5,15,14,1,9},
                {12,5,1,15,14,13,4,10,0,7,6,3,9,2,8,11},
                {13,11,7,14,12,1,3,9,5,0,15,4,8,6,2,10},
                {6,15,14,9,11,3,0,8,12,2,13,7,1,4,10,5},
                {10,2,8,4,7,6,1,5,15,11,9,14,3,12,13,0}};
        static void g(int[] v, int a, int b, int c, int d, int x, int y){
            v[a] = v[a] + v[b] + x; v[d] = Integer.rotateRight(v[d]^v[a], 16);
            v[c] = v[c] + v[d];     v[b] = Integer.rotateRight(v[b]^v[c], 12);
            v[a] = v[a] + v[b] + y; v[d] = Integer.rotateRight(v[d]^v[a], 8);
            v[c] = v[c] + v[d];     v[b] = Integer.rotateRight(v[b]^v[c], 7);
        }
        byte[] hash(byte[] m){
            int[] h = IV.clone();
            h[0] ^= 0x01010000 ^ 32;
            int blocks = (m.length + 63) / 64;
            if(blocks == 0) blocks = 1;
            long t = 0;
            for(int b=0;b<blocks;b++){
                byte[] blk = new byte[64];
                int len = Math.min(64, Math.max(0, m.length - b*64));
                if(len > 0) System.arraycopy(m, b*64, blk, 0, len);
                t += len;
                int[] v = new int[16];
                System.arraycopy(h, 0, v, 0, 8);
                System.arraycopy(IV, 0, v, 8, 8);
                v[12] ^= (int)t;
                v[13] ^= (int)(t>>>32);
                if(b == blocks-1) v[14] = ~v[14];
                for(int r=0;r<10;r++){
                    byte[] s = SIGMA[r];
                    g(v, 0, 4, 8,12, le(blk,s[0]*4), le(blk,s[1]*4));
                    g(v, 1, 5, 9,13, le(blk,s[2]*4), le(blk,s[3]*4));
                    g(v, 2, 6,10,14, le(blk,s[4]*4), le(blk,s[5]*4));
                    g(v, 3, 7,11,15, le(blk,s[6]*4), le(blk,s[7]*4));
                    g(v, 0, 5,10,15, le(blk,s[8]*4), le(blk,s[9]*4));
                    g(v, 1, 6,11,12, le(blk,s[10]*4), le(blk,s[11]*4));
                    g(v, 2, 7, 8,13, le(blk,s[12]*4), le(blk,s[13]*4));
                    g(v, 3, 4, 9,14, le(blk,s[14]*4), le(blk,s[15]*4));
                }
                for(int i=0;i<8;i++) h[i] ^= v[i] ^ v[i+8];
            }
            byte[] out = new byte[32];
            for(int i=0;i<8;i++) for(int b=0;b<4;b++) out[i*4+b] = (byte)(h[i] >>> (8*b));
            return out;
        }
        private static int le(byte[] b, int off){ return (b[off]&0xFF) | ((b[off+1]&0xFF)<<8) | ((b[off+2]&0xFF)<<16) | ((b[off+3]&0xFF)<<24); }
        public long nextLong(){
            byte[] in = new byte[8];
            long c = ctr++;
            for(int i=0;i<8;i++) in[i] = (byte)((key >>> (8*i)) ^ (c >>> (8*i)));
            byte[] out = hash(in);
            long r=0; for(int i=0;i<8;i++) r |= ((long)(out[i]&0xFF)) << (8*i);
            return r;
        }
    }

    /* 9 */ static final class Blake2bHash implements Prng {
        private long ctr=0, key; Blake2bHash(){ this(0x1234L); } Blake2bHash(long k){ key=k; }
        private static final long[] IV = {0x6a09e667f3bcc908L,0xbb67ae8584caa73bL,0x3c6ef372fe94f82bL,0xa54ff53a5f1d36f1L,
                0x510e527fade682d1L,0x9b05688c2b3e6c1fL,0x1f83d9abfb41bd6bL,0x5be0cd19137e2179L};
        private static final byte[][] SIGMA = Blake2sHash.SIGMA;
        static void g(long[] v, int a, int b, int c, int d, long x, long y){
            v[a] = v[a] + v[b] + x; v[d] = Long.rotateRight(v[d]^v[a], 32);
            v[c] = v[c] + v[d];     v[b] = Long.rotateRight(v[b]^v[c], 24);
            v[a] = v[a] + v[b] + y; v[d] = Long.rotateRight(v[d]^v[a], 16);
            v[c] = v[c] + v[d];     v[b] = Long.rotateRight(v[b]^v[c], 63);
        }
        byte[] hash(byte[] m){
            long[] h = IV.clone();
            h[0] ^= 0x01010000 ^ 64;
            int blocks = (m.length + 127) / 128;
            if(blocks == 0) blocks = 1;
            long t = 0;
            for(int b=0;b<blocks;b++){
                byte[] blk = new byte[128];
                int len = Math.min(128, Math.max(0, m.length - b*128));
                if(len > 0) System.arraycopy(m, b*128, blk, 0, len);
                t += len;
                long[] v = new long[16];
                System.arraycopy(h, 0, v, 0, 8);
                System.arraycopy(IV, 0, v, 8, 8);
                v[12] ^= t;
                if(b == blocks-1) v[14] = ~v[14];
                for(int r=0;r<12;r++){
                    byte[] s = SIGMA[r % 10];
                    g(v,0,4, 8,12, le(blk,s[0]*8), le(blk,s[1]*8));
                    g(v,1,5, 9,13, le(blk,s[2]*8), le(blk,s[3]*8));
                    g(v,2,6,10,14, le(blk,s[4]*8), le(blk,s[5]*8));
                    g(v,3,7,11,15, le(blk,s[6]*8), le(blk,s[7]*8));
                    g(v,0,5,10,15, le(blk,s[8]*8), le(blk,s[9]*8));
                    g(v,1,6,11,12, le(blk,s[10]*8), le(blk,s[11]*8));
                    g(v,2,7, 8,13, le(blk,s[12]*8), le(blk,s[13]*8));
                    g(v,3,4, 9,14, le(blk,s[14]*8), le(blk,s[15]*8));
                }
                for(int i=0;i<8;i++) h[i] ^= v[i] ^ v[i+8];
            }
            byte[] out = new byte[64];
            for(int i=0;i<8;i++) for(int b=0;b<8;b++) out[i*8+b] = (byte)(h[i] >>> (8*b));
            return out;
        }
        private static long le(byte[] b, int off){ long v=0; for(int i=7;i>=0;i--) v = (v<<8) | (b[off+i]&0xFF); return v; }
        public long nextLong(){
            byte[] in = new byte[8];
            long c = ctr++;
            for(int i=0;i<8;i++) in[i] = (byte)((key >>> (8*i)) ^ (c >>> (8*i)));
            byte[] out = hash(in);
            long r=0; for(int i=0;i<8;i++) r |= ((long)(out[i]&0xFF)) << (8*i);
            return r;
        }
    }

    /* 10 */ static final class Blake3Hash implements Prng {
        private long ctr=0, key; Blake3Hash(){ this(0x1234L); } Blake3Hash(long k){ key=k; }
        public long nextLong(){
            byte[] in = new byte[8];
            long c = ctr++;
            for(int i=0;i<8;i++) in[i] = (byte)((key >>> (8*i)) ^ (c >>> (8*i)));
            byte[] out = Blake2bHash.IV == null ? null : null;
            // BLAKE3 core with 7 rounds over 16-word state; absorb 8 bytes
            int[] v = new int[16];
            for(int i=0;i<8;i++) v[i] = (int)(Blake2sHash.IV[i]);
            v[8] = 0x01010000; v[9]=0; v[10]=0; v[11]=0;
            v[12] = (int)c; v[13] = (int)(c>>>32);
            v[14] = in.length; v[15] = 0;
            for(int i=0;i<2;i++) v[i] ^= le(in, i*4);
            int[][] msg = {
                    {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15},
                    {2,6,3,10,7,0,4,13,1,11,12,5,9,14,15,8},
                    {3,4,10,12,13,2,7,14,6,5,9,0,11,15,8,1},
                    {10,7,12,9,14,3,13,15,4,0,11,2,5,8,1,6},
                    {12,13,9,11,15,10,14,8,7,2,5,3,0,1,6,4},
                    {9,14,11,5,8,12,15,1,13,3,0,10,2,6,4,7},
                    {11,15,5,0,1,9,8,6,14,10,2,12,3,4,7,13}};
            for(int r=0;r<7;r++){
                int[] m = msg[r];
                Blake2sHash.g(v,0,4, 8,12, v[m[0]], v[m[1]]);
                Blake2sHash.g(v,1,5, 9,13, v[m[2]], v[m[3]]);
                Blake2sHash.g(v,2,6,10,14, v[m[4]], v[m[5]]);
                Blake2sHash.g(v,3,7,11,15, v[m[6]], v[m[7]]);
                Blake2sHash.g(v,0,5,10,15, v[m[8]], v[m[9]]);
                Blake2sHash.g(v,1,6,11,12, v[m[10]], v[m[11]]);
                Blake2sHash.g(v,2,7, 8,13, v[m[12]], v[m[13]]);
                Blake2sHash.g(v,3,4, 9,14, v[m[14]], v[m[15]]);
            }
            long r = 0;
            for(int i=0;i<4;i++) r |= ((long)(v[i] ^ v[i+8])) << (16*i);
            return r;
        }
        private static int le(byte[] b, int off){ return (b[off]&0xFF) | ((b[off+1]&0xFF)<<8) | ((b[off+2]&0xFF)<<16) | ((b[off+3]&0xFF)<<24); }
    }

    /* 11 */ static final class RIPEMD160Hash implements Prng {
        private long ctr=0, key; RIPEMD160Hash(){ this(0x1234L); } RIPEMD160Hash(long k){ key=k; }
        public long nextLong(){
            byte[] in = new byte[8];
            long c = ctr++;
            for(int i=0;i<8;i++) in[i] = (byte)((key >>> (8*i)) ^ (c >>> (8*i)));
            // RIPEMD-160 core
            int[] r1 = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,7,4,13,1,10,6,15,3,12,0,9,5,2,14,11,8,
                    3,10,14,4,9,15,8,1,2,7,0,6,13,11,5,12,1,9,11,10,0,8,12,4,13,3,7,15,14,5,6,2,
                    4,0,5,9,7,12,2,10,14,1,3,8,11,6,15,13};
            int[] r2 = {5,14,7,0,9,2,11,4,13,6,15,8,1,10,3,12,6,11,3,7,0,13,5,10,14,15,8,12,4,9,1,2,
                    15,5,1,3,7,14,6,9,11,8,12,2,10,0,4,13,8,6,4,1,3,11,15,0,5,12,2,13,9,7,10,14,
                    12,15,10,4,1,5,8,7,6,2,13,14,0,3,9,11};
            int[] s1 = {11,14,15,12,5,8,7,9,11,13,14,15,6,7,9,8,7,6,8,13,11,9,7,15,7,12,15,9,11,7,13,12,
                    11,13,6,7,14,9,13,15,14,8,13,6,5,12,7,5,11,12,14,15,14,15,9,8,9,14,5,6,8,6,5,12,
                    9,15,5,11,6,8,13,12,5,12,13,14,11,8,5,6};
            int[] s2 = {8,9,9,11,13,15,15,5,7,7,8,11,14,14,12,6,9,13,15,7,12,8,9,11,7,7,12,7,6,15,13,11,
                    9,7,15,11,8,6,6,14,12,13,5,14,13,13,7,5,15,5,8,11,14,14,6,14,6,9,12,9,12,5,15,8,
                    8,5,12,9,12,5,14,6,8,13,6,5,15,13,11,11};
            int[] K1 = {0,0x5a827999,0x6ed9eba1,0x8f1bbcdc,0xa953fd4e};
            int[] K2 = {0x50a28be6,0x5c4dd124,0x6d703ef3,0x7a6d76e9,0};
            int h0=0x67452301,h1=0xefcdab89,h2=0x98badcfe,h3=0x10325476,h4=0xc3d2e1f0;
            int padLen = ((in.length + 8) / 64 + 1) * 64;
            byte[] msg = new byte[padLen];
            System.arraycopy(in,0,msg,0,in.length);
            msg[in.length] = (byte)0x80;
            long bitLen = (long)in.length * 8;
            for(int i=0;i<8;i++) msg[padLen-8+i] = (byte)(bitLen >>> (8*i));
            for(int off=0; off<padLen; off+=64){
                int[] X = new int[16];
                for(int i=0;i<16;i++) X[i] = (msg[off+i*4]&0xFF) | ((msg[off+i*4+1]&0xFF)<<8) | ((msg[off+i*4+2]&0xFF)<<16) | ((msg[off+i*4+3]&0xFF)<<24);
                int al=h0,bl=h1,cl=h2,dl=h3,el=h4;
                int ar=h0,br=h1,cr=h2,dr=h3,er=h4;
                for(int i=0;i<80;i++){
                    int t;
                    int f1 = i<16 ? bl^cl^dl : i<32 ? (bl&cl)|(~bl&dl) : i<48 ? (bl|~cl)^dl : i<64 ? (bl&dl)|(cl&~dl) : bl^(cl|~dl);
                    t = Integer.rotateLeft(al + f1 + X[r1[i]] + K1[i/16], s1[i]) + el;
                    al=el; el=dl; dl=Integer.rotateLeft(cl,10); cl=bl; bl=t;
                    int f2 = i<16 ? br^(cr|~dr) : i<32 ? (br&dr)|(cr&~dr) : i<48 ? (br|~cr)^dr : i<64 ? (br&cr)|(~br&dr) : br^cr^dr;
                    t = Integer.rotateLeft(ar + f2 + X[r2[i]] + K2[i/16], s2[i]) + er;
                    ar=er; er=dr; dr=Integer.rotateLeft(cr,10); cr=br; br=t;
                }
                int t = h1 + cl + dr;
                h1 = h2 + dl + er;
                h2 = h3 + el + ar;
                h3 = h4 + al + br;
                h4 = h0 + bl + cr;
                h0 = t;
            }
            long r = 0;
            r |= ((long)h0 & 0xFFFFFFFFL);
            r |= ((long)h1 & 0xFFFFFFFFL) << 32;
            return r ^ ((long)h2 << 8) ^ ((long)h3 << 16) ^ ((long)h4 << 24);
        }
    }

    /* 12 */ static final class WhirlpoolHash implements Prng {
        private long ctr=0, key; WhirlpoolHash(){ this(0x1234L); } WhirlpoolHash(long k){ key=k; }
        // Whirlpool S-box (first 16 entries; repeated for demo via mix)
        private static final int[] SBOX = new int[256];
        static {
            long x = 0x1F;
            for(int i=0;i<256;i++){
                SBOX[i] = (int)(x & 0xFF);
                x ^= (x << 1) ^ ((x & 0x80)!=0 ? 0x11D : 0);
                x &= 0xFF;
                x ^= (x << 4) & 0xFF;
                x ^= (x << 5) & 0xFF;
                x ^= (x << 6) & 0xFF;
            }
        }
        public long nextLong(){
            byte[] in = new byte[8];
            long c = ctr++;
            for(int i=0;i<8;i++) in[i] = (byte)((key >>> (8*i)) ^ (c >>> (8*i)));
            long[] H = new long[8];
            long[] K = new long[8];
            for(int i=0;i<8;i++) H[i] = 0;
            long[] block = new long[8];
            for(int i=0;i<8;i++){
                long v = 0;
                for(int b=0;b<8;b++) v = (v<<8) | (in[(i*8+b)%8]&0xFF);
                block[i] = v;
            }
            long[] rc = {0x1823c6e887b8014fL,0x36a6d2f5796f9152L,0x60bc9b8ea30c7b35L,
                    0x1de0d7c22e4bfe57L,0x157737e59ff04adaL,0x58c9290ab1a06b45L,
                    0x1d5d07f4b1b1b6c1L,0x110ee9a5cdc1a9c3L,0x1a5e8c8f2aeb75e1L,
                    0x0f9d5c4b1a8f2e6dL};
            for(int round=0; round<10; round++){
                for(int i=0;i<8;i++){
                    int s = (int)((K[i] >>> 56) & 0xFF);
                    long t = 0;
                    for(int b=0;b<8;b++){
                        int sb = (int)((K[(i+b)%8] >>> (56 - 8*b)) & 0xFF);
                        t = (t<<8) | (SBOX[(sb + b*3) & 0xFF] & 0xFF);
                    }
                    K[i] = t;
                }
                K[0] ^= rc[round];
            }
            long r = 0;
            for(int i=0;i<4;i++) r ^= H[i] ^ block[i] ^ K[i];
            r ^= c * 0x9E3779B97F4A7C15L;
            return r ^ Long.rotateLeft(r, 17);
        }
    }

    /* 13 */ static final class TigerHash implements Prng {
        private long ctr=0, key; TigerHash(){ this(0x1234L); } TigerHash(long k){ key=k; }
        private static final long[] T = new long[1024];
        static {
            long x = 0x02;
            for(int i=0;i<1024;i++){
                T[i] = x * 0x9E3779B97F4A7C15L + i;
                x = Long.rotateLeft(x, 7) ^ (i * 0xBF58476D1CE4E5B9L);
            }
        }
        static void round(long[] a, long[] b, long[] c, long[] x, int mul){
            long aa=a[0], bb=b[0], cc=c[0];
            cc ^= x[0];
            aa -= T[(int)(cc & 0x3FF)];
            bb += T[(int)((cc>>>10) & 0x3FF)];
            bb *= mul;
            aa ^= T[(int)((bb>>>8) & 0x3FF)];
            cc += T[(int)((bb>>>16) & 0x3FF)];
            cc *= mul;
            a[0]=aa; b[0]=bb; c[0]=cc;
        }
        public long nextLong(){
            byte[] in = new byte[8];
            long c = ctr++;
            for(int i=0;i<8;i++) in[i] = (byte)((key >>> (8*i)) ^ (c >>> (8*i)));
            long[] a = {0x0123456789ABCDEFL};
            long[] b = {0xFEDCBA9876543210L};
            long[] cc = {0xF096A5B4C3B2E187L};
            long[] x = new long[8];
            for(int i=0;i<8;i++) x[i] = in[i%8] & 0xFF;
            for(int pass=0; pass<3; pass++){
                for(int i=0;i<8;i++){
                    round(a, b, cc, new long[]{x[i]}, 5 + i);
                }
            }
            return a[0] ^ b[0] ^ cc[0] ^ c;
        }
    }

    /* 14 */ static final class Skein256Hash implements Prng {
        private long ctr=0, key; Skein256Hash(){ this(0x1234L); } Skein256Hash(long k){ key=k; }
        static void threefish(long[] v, long[] k, long[] t, long[] rc){
            long k0=k[0], k1=k[1], k2=k[2], k3=k[3], k4=k[0]^k[1]^k[2]^k[3]^0x1BD11BDAA9FC1A22L;
            long t0=t[0], t1=t[1], t2=t0^t1;
            v[0]+=k0; v[1]+=k1; v[2]+=k2; v[3]+=k3;
            for(int r=0; r<32; r++){
                v[0]+=v[1]; v[1]=Long.rotateLeft(v[1], 5) ^ v[0];
                v[2]+=v[3]; v[3]=Long.rotateLeft(v[3], 56) ^ v[2];
                v[0]+=v[3]; v[3]=Long.rotateLeft(v[3], 36) ^ v[0];
                v[2]+=v[1]; v[1]=Long.rotateLeft(v[1], 28) ^ v[2];
                v[0]+=v[1]; v[1]=Long.rotateLeft(v[1], 13) ^ v[0];
                v[2]+=v[3]; v[3]=Long.rotateLeft(v[3], 46) ^ v[2];
                v[0]+=v[3]; v[3]=Long.rotateLeft(v[3], 58) ^ v[0];
                v[2]+=v[1]; v[1]=Long.rotateLeft(v[1], 44) ^ v[2];
                v[0] += k[(r+1)%5];
                v[1] += k[(r+2)%5] + (r+1);
                v[2] += k[(r+3)%5] + (r+1);
                v[3] += k[(r+4)%5] + (r+1);
                if(r==31){
                    v[1] += t0;
                    v[2] += t1;
                    v[3] += t2;
                }
            }
        }
        public long nextLong(){
            byte[] in = new byte[8];
            long c = ctr++;
            for(int i=0;i<8;i++) in[i] = (byte)((key >>> (8*i)) ^ (c >>> (8*i)));
            long[] v = new long[4];
            for(int i=0;i<4;i++) v[i] = 0;
            for(int i=0;i<4;i++){
                long b = 0;
                for(int j=0;j<2;j++) b = (b<<8) | (in[(i*2+j)%8]&0xFF);
                v[i] = b;
            }
            long[] k = {0x1234567890ABCDEFL, 0x0FEDCBA0987654321L, 0x1122334455667788L, 0x99AABBCCDDEEFF00L};
            long[] t = {8, 0x40};
            long[] rc = new long[32];
            threefish(v, k, t, rc);
            return v[0] ^ v[1] ^ v[2] ^ v[3] ^ c;
        }
    }

    /* 15 */ static final class GroestlHash implements Prng {
        private long ctr=0, key; GroestlHash(){ this(0x1234L); } GroestlHash(long k){ key=k; }
        public long nextLong(){
            byte[] in = new byte[8];
            long c = ctr++;
            for(int i=0;i<8;i++) in[i] = (byte)((key >>> (8*i)) ^ (c >>> (8*i)));
            // Simplified Groestl-like P permutation on 8 bytes
            int[] p = new int[64];
            for(int i=0;i<64;i++) p[i] = (in[i%8] & 0xFF) ^ i;
            int[] sbox = new int[256];
            for(int i=0;i<256;i++) sbox[i] = ((i*0x1B) ^ ((i<<2)|(i>>>6)) ^ (i*i)) & 0xFF;
            for(int r=0; r<10; r++){
                int[] q = new int[64];
                for(int i=0;i<64;i++) q[i] = sbox[p[i]];
                for(int i=0;i<64;i++) p[i] = q[(i*8 + i/8) % 64];
                for(int i=0;i<64;i++) p[i] ^= (r*17 + i*3) & 0xFF;
            }
            long r = 0;
            for(int i=0;i<8;i++) r |= ((long)(p[i] & 0xFF)) << (i*8);
            return r ^ c;
        }
    }

    /* 16 */ static final class JHHash implements Prng {
        private long ctr=0, key; JHHash(){ this(0x1234L); } JHHash(long k){ key=k; }
        public long nextLong(){
            byte[] in = new byte[8];
            long c = ctr++;
            for(int i=0;i<8;i++) in[i] = (byte)((key >>> (8*i)) ^ (c >>> (8*i)));
            int[] h = new int[16];
            for(int i=0;i<16;i++) h[i] = 0x6A09E667 ^ (i*0x9E3779B9);
            for(int i=0;i<8;i++) h[i] ^= in[i]&0xFF;
            int[] sbox = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,
                          3,7,11,15,1,5,9,13,2,6,10,14,0,4,8,12};
            int[][] L = {{4,5,6,7,8,9,10,11,12,13,14,15,0,1,2,3},
                         {1,3,5,7,9,11,13,15,0,2,4,6,8,10,12,14},
                         {7,6,5,4,3,2,1,0,15,14,13,12,11,10,9,8}};
            for(int round=0; round<42; round++){
                int[] t = new int[16];
                for(int i=0;i<16;i++) t[i] = sbox[(h[i] ^ (round*3+i)) & 31] | (sbox[((h[i]>>>5) + i) & 31] << 4);
                for(int i=0;i<16;i++) h[i] = (t[i] ^ t[L[0][i]] ^ t[L[1][i]] ^ t[L[2][i]]) & 0xFF;
                h[round % 16] ^= round;
            }
            long r = 0;
            for(int i=0;i<8;i++) r |= ((long)(h[i] & 0xFF)) << (i*8);
            return r ^ c;
        }
    }

    /* 17 */ static final class SipHash24 implements Prng {
        private long ctr=0, k0, k1; SipHash24(){ this(0x1234L, 0x5678L); } SipHash24(long a, long b){ k0=a; k1=b; }
        private static long rotl(long x, int b){ return (x<<b)|(x>>>(64-b)); }
        private static void sipround(long[] v){
            v[0] += v[1]; v[1] = rotl(v[1],13); v[1] ^= v[0]; v[0] = rotl(v[0],32);
            v[2] += v[3]; v[3] = rotl(v[3],16); v[3] ^= v[2];
            v[0] += v[3]; v[3] = rotl(v[3],21); v[3] ^= v[0];
            v[2] += v[1]; v[1] = rotl(v[1],17); v[1] ^= v[2]; v[2] = rotl(v[2],32);
        }
        public long nextLong(){
            long m = ctr++ * 0x9E3779B97F4A7C15L ^ k0;
            long[] v = {k0 ^ 0x736f6d6570736575L, k1 ^ 0x646f72616e646f6dL,
                        k0 ^ 0x6c7967656e657261L, k1 ^ 0x7465646279746573L};
            v[3] ^= m;
            sipround(v); sipround(v);
            v[0] ^= m;
            v[2] ^= 0xFF;
            sipround(v); sipround(v); sipround(v); sipround(v);
            return v[0] ^ v[1] ^ v[2] ^ v[3];
        }
    }

    /* 18 */ static final class Poly1305Hash implements Prng {
        private long ctr=0, key; Poly1305Hash(){ this(0x1234L); } Poly1305Hash(long k){ key=k; }
        public long nextLong(){
            long c = ctr++;
            // Poly1305-like: r = key & 0x0ffffffc0ffffffc, s = key
            long r = key & 0x0FFFFFFC0FFFFFFCL;
            long s = key ^ 0x9E3779B97F4A7C15L;
            long acc = 0;
            long[] msg = {c, key ^ c, c * 2, key + c};
            for(long m : msg){
                acc += (m & 0xFFFFFFFFL) & 0xFFFFFFFFL;
                acc = (acc * (r & 0xFFFFFFFFL)) & 0xFFFFFFFFFFFFFFFL;
                acc += s & 0xFFFFFFFFL;
            }
            return acc ^ (acc >>> 17) ^ Long.rotateLeft(acc, 23);
        }
    }

    /* 19 */ static final class HighwayHash implements Prng {
        private long ctr=0, k0, k1, k2, k3;
        HighwayHash(){ this(1,2,3,4); }
        HighwayHash(long a, long b, long c, long d){ k0=a;k1=b;k2=c;k3=d; }
        public long nextLong(){
            long c = ctr++;
            long[] v = {k0^c, k1+c, k2^Long.rotateLeft(c,17), k3+Long.rotateLeft(c,31)};
            for(int r=0; r<4; r++){
                v[0] = (v[0] + v[1]) * 0x9E3779B97F4A7C15L;
                v[2] = (v[2] + v[3]) * 0xBF58476D1CE4E5B9L;
                v[1] ^= Long.rotateLeft(v[0], 17);
                v[3] ^= Long.rotateLeft(v[2], 29);
                v[0] = v[0] ^ v[3];
                v[2] = v[2] ^ v[1];
            }
            return v[0] ^ v[1] ^ v[2] ^ v[3];
        }
    }

    /* 20 */ static final class KangarooTwelve implements Prng {
        private long ctr=0, key; KangarooTwelve(){ this(0x1234L); } KangarooTwelve(long k){ key=k; }
        public long nextLong(){
            byte[] in = new byte[8];
            long c = ctr++;
            for(int i=0;i<8;i++) in[i] = (byte)((key >>> (8*i)) ^ (c >>> (8*i)));
            long[] s = new long[25];
            for(int i=0;i<8;i++) s[i/8] ^= ((long)(in[i]&0xFF)) << (8*(i%8));
            s[8/8] ^= 0x1F;
            s[167/8] ^= 0x80;
            SHA3_256Hash.keccakF(s);
            // Second block with domain separation 0x0B (TurboSHAKE)
            for(int i=0;i<8;i++) s[i/8] ^= ((long)(in[i]&0xFF)) << (8*(i%8));
            s[8/8] ^= 0x0B;
            SHA3_256Hash.keccakF(s);
            long r = 0;
            for(int i=0;i<8;i++) r |= ((long)((s[i/8] >>> (8*(i%8))) & 0xFF)) << (8*i);
            return r;
        }
    }

    /* 21 */ static final class RC4 implements Prng {
        private final int[] s = new int[256]; private int i=0, j=0;
        RC4(long seed){ init(seed); }
        private void init(long seed){
            for(int k=0;k<256;k++) s[k]=k;
            long x = seed;
            for(int k=0;k<256;k++){ x = x*6364136223846793005L + 1442695040888963407L; j = (j + s[k] + (int)(x>>>56)) & 255; int t=s[k]; s[k]=s[j]; s[j]=t; }
            i=0; j=0;
        }
        public long nextLong(){
            long r = 0;
            for(int k=0;k<8;k++){
                i = (i+1)&255; j = (j+s[i])&255; int t=s[i]; s[i]=s[j]; s[j]=t;
                r |= ((long)(s[(s[i]+s[j])&255] & 0xFF)) << (8*k);
            }
            return r;
        }
    }

    /* 22 */ static final class RC4A implements Prng {
        private final int[] s1 = new int[256], s2 = new int[256];
        private int i1=0, j1=0, i2=0, j2=0;
        RC4A(long seed){ init(seed); }
        private void init(long seed){
            for(int k=0;k<256;k++){ s1[k]=k; s2[k]=k; }
            long x = seed;
            for(int k=0;k<256;k++){ x=x*6364136223846793005L+1442695040888963407L; j1=(j1+s1[k]+(int)(x>>>56))&255; int t=s1[k]; s1[k]=s1[j1]; s1[j1]=t; }
            for(int k=0;k<256;k++){ x=x*6364136223846793005L+1442695040888963407L; j2=(j2+s2[k]+(int)(x>>>56))&255; int t=s2[k]; s2[k]=s2[j2]; s2[j2]=t; }
        }
        public long nextLong(){
            long r=0;
            for(int k=0;k<8;k++){
                i1=(i1+1)&255; j1=(j1+s1[i1])&255; int t=s1[i1]; s1[i1]=s1[j1]; s1[j1]=t;
                int a = s2[(s1[i1]+s1[j1])&255];
                i2=(i2+1)&255; j2=(j2+s2[i2])&255; t=s2[i2]; s2[i2]=s2[j2]; s2[j2]=t;
                int b = s1[(s2[i2]+s2[j2])&255];
                r |= ((long)((a^b)&0xFF)) << (8*k);
            }
            return r;
        }
    }

    /* 23 */ static final class Spritz implements Prng {
        private final int[] s = new int[256]; private int i=0, j=0, k=0, z=0, w=0;
        Spritz(long seed){ init(seed); }
        private void init(long seed){
            for(int x=0;x<256;x++) s[x]=x;
            long t = seed;
            for(int x=0;x<256;x++){ t=t*6364136223846793005L+1442695040888963407L; j=(j+s[x]+(int)(t>>>56))&255; int tmp=s[x]; s[x]=s[j]; s[j]=tmp; }
            i=0; j=0; k=0; z=0; w=0;
        }
        private void update(){ w = (w+1)&255; int t = s[w]; s[w]=s[z]; s[z]=t; }
        private void shuffle(){
            update();
            int t1 = s[(z+s[i])&255]; s[z]=t1; s[i]=s[j]; s[j]=t1;
        }
        private int drip(){
            if(z==0) shuffle();
            int t = s[z]; z=(z+1)&255;
            return t;
        }
        public long nextLong(){
            long r=0;
            for(int b=0;b<8;b++) r |= ((long)(drip()&0xFF)) << (8*b);
            return r;
        }
    }

    /* 24 */ static final class A5_1 implements Prng {
        private int r1, r2, r3;
        A5_1(long seed){ init(seed); }
        private void init(long seed){
            r1 = (int)(seed ^ (seed>>>32)) & 0x7FFFFF;
            r2 = (int)(seed*2) & 0x3FFFFF;
            r3 = (int)(seed*3) & 0x7FFFFF;
            if(r1==0) r1=1; if(r2==0) r2=1; if(r3==0) r3=1;
        }
        private int parity(int x){ int b=0; while(x!=0){ b^=x&1; x>>>=1; } return b; }
        public long nextLong(){
            long r=0;
            for(int k=0;k<64;k++){
                int c1 = ((r1>>>8)&1);
                int c2 = ((r2>>>10)&1);
                int c3 = ((r3>>>10)&1);
                int maj = (c1 + c2 + c3) >= 2 ? 1 : 0;
                if(c1==maj){ int f = ((r1>>>13)^(r1>>>16)^(r1>>>17)^(r1>>>18)) & 1; r1 = ((r1<<1)|f)&0x7FFFFF; }
                if(c2==maj){ int f = ((r2>>>20)^(r2>>>21)) & 1; r2 = ((r2<<1)|f)&0x3FFFFF; }
                if(c3==maj){ int f = ((r3>>>7)^(r3>>>20)^(r3>>>21)^(r3>>>22)) & 1; r3 = ((r3<<1)|f)&0x7FFFFF; }
                r |= ((long)((r1 ^ r2 ^ r3)&1)) << k;
            }
            return r;
        }
    }

    /* 25 */ static final class A5_2 implements Prng {
        private int r1, r2, r3;
        A5_2(long seed){ init(seed); }
        private void init(long seed){
            r1 = (int)(seed*7) & 0x7FFFFF; if(r1==0) r1=1;
            r2 = (int)(seed*11) & 0x3FFFFF; if(r2==0) r2=1;
            r3 = (int)(seed*13) & 0x7FFFFF; if(r3==0) r3=1;
        }
        public long nextLong(){
            long r=0;
            for(int k=0;k<64;k++){
                int c1=((r1>>>8)&1), c2=((r2>>>10)&1), c3=((r3>>>10)&1);
                int maj = (c1+c2+c3)>=2 ? 1 : 0;
                if(c1==maj){ r1 = ((r1<<1) | ((r1>>>18)^(r1>>>17)^(r1>>>16)^(r1>>>13))&1) & 0x7FFFFF; }
                if(c2==maj){ r2 = ((r2<<1) | ((r2>>>21)^(r2>>>20))&1) & 0x3FFFFF; }
                if(c3==maj){ r3 = ((r3<<1) | ((r3>>>22)^(r3>>>21)^(r3>>>20)^(r3>>>7))&1) & 0x7FFFFF; }
                r |= ((long)((r1 & r2) | (~r1 & r3) & 1)) << k;
            }
            return r;
        }
    }

    /* 26 */ static final class Trivium implements Prng {
        private int a,b,c; private final int[] s = new int[288];
        Trivium(long seed){ init(seed); }
        private void init(long seed){
            long x = seed;
            for(int i=0;i<288;i++){ x=x*6364136223846793005L+1442695040888963407L; s[i]=(int)(x>>>56)&1; }
        }
        public long nextLong(){
            long out=0;
            for(int k=0;k<64;k++){
                int t1 = s[65] ^ s[92];
                int t2 = s[161] ^ s[176];
                int t3 = s[242] ^ s[287];
                int y1 = t1 ^ (s[90] & s[91]) ^ s[170];
                int y2 = t2 ^ (s[174] & s[175]) ^ s[263];
                int y3 = t3 ^ (s[285] & s[286]) ^ s[68];
                out |= ((long)y1) << k;
                int t4 = t1 ^ (s[170] & s[171]) ^ s[90];
                int t5 = t2 ^ (s[263] & s[264]) ^ s[174];
                int t6 = t3 ^ (s[68] & s[69]) ^ s[285];
                int[] n = new int[288];
                for(int i=0;i<285;i++) n[i]=s[i+1];
                n[285]=t4; n[286]=t5; n[287]=t6;
                System.arraycopy(n, 0, s, 0, 288);
            }
            return out;
        }
    }

    /* 27 */ static final class Grain128 implements Prng {
        private long lfsr, nfsr;
        Grain128(long seed){ init(seed); }
        private void init(long seed){
            lfsr = seed | 1L;
            nfsr = (seed ^ 0x9E3779B97F4A7C15L) | 1L;
        }
        public long nextLong(){
            long out = 0;
            for(int k=0;k<64;k++){
                long b0 = (nfsr>>>0)&1, b26 = (nfsr>>>26)&1, b56 = (nfsr>>>56)&1, b91 = (nfsr>>>91)&1, b96 = (nfsr>>>96)&1;
                long l0 = (lfsr>>>0)&1, l67 = (lfsr>>>67)&1, l89 = (lfsr>>>89)&1, l127 = (lfsr>>>127)&1;
                long newN = b0 ^ b26 ^ b56 ^ b91 ^ b96 ^ (b0 & b67bit(lfsr)) ^ (b26 & b56);
                long newL = l0 ^ l67 ^ l89 ^ l127;
                long y = ((nfsr>>>0)&1) ^ ((lfsr>>>93)&1) ^ b(lfsr, 2) ^ b(lfsr, 15) ^ b(lfsr, 36) ^ b(lfsr, 45) ^ b(lfsr, 64);
                out |= y << k;
                nfsr = (nfsr>>>1) | (newN<<127);
                lfsr = (lfsr>>>1) | (newL<<127);
            }
            return out;
        }
        private static long b(long x, int n){ return (x>>>n)&1; }
        private static long b67bit(long x){ return ((x>>>67)&1); }
    }

    /* 28 */ static final class Grain128a implements Prng {
        private long lfsr, nfsr;
        Grain128a(long seed){ init(seed); }
        private void init(long seed){ lfsr = (seed|1)^0xA5A5A5A5A5A5A5A5L; nfsr = ((seed*3)|1)^0x5A5A5A5A5A5A5A5AL; }
        public long nextLong(){
            long out=0;
            for(int k=0;k<64;k++){
                long n0=(nfsr>>>0)&1, n7=(nfsr>>>7)&1, n38=(nfsr>>>38)&1, n70=(nfsr>>>70)&1, n81=(nfsr>>>81)&1, n96=(nfsr>>>96)&1;
                long l0=(lfsr>>>0)&1, l26=(lfsr>>>26)&1, l56=(lfsr>>>56)&1, l91=(lfsr>>>91)&1, l96=(lfsr>>>96)&1, l3=(lfsr>>>3)&1, l67=(lfsr>>>67)&1, l11=(lfsr>>>11)&1, l13=(lfsr>>>13)&1;
                long newN = n0 ^ n7 ^ n38 ^ n70 ^ n81 ^ n96 ^ (n0&l3) ^ (n7&l67) ^ (n38&l11) ^ (n70&l13);
                long newL = l0 ^ l26 ^ l56 ^ l91 ^ l96 ^ (l3&l67);
                long y = ((lfsr>>>8)&1) ^ ((lfsr>>>13)&1) ^ ((lfsr>>>20)&1) ^ ((nfsr>>>42)&1) ^ ((nfsr>>>60)&1) ^ ((nfsr>>>63)&1) ^ (l3&l67);
                out |= y << k;
                nfsr = (nfsr>>>1) | (newN<<127);
                lfsr = (lfsr>>>1) | (newL<<127);
            }
            return out;
        }
    }

    /* 29 */ static final class MICKEY128 implements Prng {
        private long r, s; private int rIdx=0, sIdx=0;
        MICKEY128(long seed){ r = seed|1; s = (seed*7)|1; }
        private int nextR(){
            int out = (int)((r>>>rIdx)&1);
            int t = ((int)((r>>>0)&1) ^ (int)((r>>>27)&1) ^ (int)((r>>>56)&1)) & 1;
            r = ((r>>>1) | ((long)t<<127));
            rIdx = (rIdx+1)&127;
            return out;
        }
        private int nextS(){
            int out = (int)((s>>>sIdx)&1);
            int t = ((int)((s>>>0)&1) ^ (int)((s>>>37)&1) ^ (int)((s>>>80)&1) ^ (int)((s>>>99)&1)) & 1;
            s = ((s>>>1) | ((long)t<<127));
            sIdx = (sIdx+1)&127;
            return out;
        }
        public long nextLong(){
            long out=0;
            for(int k=0;k<64;k++){
                int rr = nextR();
                int ss = nextS();
                int mix = (int)(r & s & 1);
                out |= ((long)((rr ^ ss ^ mix) & 1)) << k;
                if(((r>>>50)&1)!=0){ s = (s>>>1) | ((long)(r&1)<<127); }
                r = (r>>>1) | ((long)((r ^ s)&1)<<127);
            }
            return out;
        }
    }

    /* 30 */ static final class SNOW3G implements Prng {
        private int[] lfsr = new int[16];
        private int fsm1=0, fsm2=0, fsm3=0;
        SNOW3G(long seed){ init(seed); }
        private void init(long seed){
            long x = seed;
            for(int i=0;i<16;i++){ x=x*6364136223846793005L+1442695040888963407L; lfsr[i]=(int)(x>>>32); }
        }
        private int s1(int x){ return ((x<<1)|(x>>>31)) ^ ((x<<15)|(x>>>17)) ^ ((x<<23)|(x>>>9)) ^ ((x<<29)|(x>>>3)) ^ ((x<<7)|(x>>>25)); }
        private int s2(int x){ return ((x<<3)|(x>>>29)) ^ ((x<<13)|(x>>>19)) ^ ((x<<21)|(x>>>11)) ^ ((x<<27)|(x>>>5)) ^ ((x<<9)|(x>>>23)); }
        public long nextLong(){
            long out=0;
            for(int k=0;k<16;k++){
                int f = (fsm1 + ((fsm2 + fsm3) ^ (fsm1>>>0))) & 0xFFFFFFFF;
                int f2 = s1((fsm1 + f) & 0xFFFFFFFF) ^ ((fsm2 + f)&0xFFFFFFFF);
                int r = (lfsr[0] ^ lfsr[5]) & 0xFFFFFFFF;
                out |= ((long)((f2 ^ r) & 0xFFFFF)) << (k*4);
                fsm3 = fsm2; fsm2 = s2(fsm1); fsm1 = f;
                int newL = lfsr[15];
                for(int i=15;i>0;i--) lfsr[i]=lfsr[i-1];
                lfsr[0] = newL;
            }
            return out;
        }
    }

    /* 31 */ static final class ZUC implements Prng {
        private final int[] lfsr = new int[16];
        private final int[] r = new int[2];
        ZUC(long seed){ init(seed); }
        private void init(long seed){
            long x = seed;
            for(int i=0;i<16;i++){ x=x*6364136223846793005L+1442695040888963407L; lfsr[i]=(int)(x>>>32)|1; }
            r[0]=0; r[1]=0;
        }
        private static int sbox(int x){ return ((x*0x1B) ^ (x>>>4) ^ ((x<<3)|(x>>>5))) & 0xFF; }
        public long nextLong(){
            long out=0;
            for(int k=0;k<16;k++){
                int x0 = ((lfsr[15]&0x7FFF)<<16) | (lfsr[14]&0xFFFF);
                int x1 = ((lfsr[11]&0xFFFF)<<16) | (lfsr[9]&0xFFFF);
                int x2 = ((lfsr[7]&0xFFFF)<<16) | (lfsr[5]&0xFFFF);
                int f = (x0 ^ r[0]) + r[1] & 0xFFFFFFFF;
                int w = f ^ x1 ^ x2;
                int z = (sbox((w>>>24)&0xFF)<<24) | (sbox((w>>>16)&0xFF)<<16) | (sbox((w>>>8)&0xFF)<<8) | sbox(w&0xFF);
                out |= ((long)z & 0xFFFFFFFFL) << (k*4);
                r[1] = r[0];
                r[0] = f;
                int newL = ((lfsr[0]<<8) | (lfsr[0]>>>23)) ^ ((lfsr[4]<<20)|(lfsr[4]>>>11)) ^ ((lfsr[10]<<30)|(lfsr[10]>>>1)) ^ ((lfsr[13]<<11)|(lfsr[13]>>>20)) ^ (lfsr[15]);
                for(int i=15;i>0;i--) lfsr[i]=lfsr[i-1];
                lfsr[0]=newL;
            }
            return out;
        }
    }

    /* 32 */ static final class Salsa20_8 implements Prng {
        private int[] s = new int[16]; private int cnt = 0;
        Salsa20_8(long seed){ init(seed, 8); }
        private void init(long seed, int rounds){
            long x = seed;
            for(int k=0;k<16;k++){ x=x*6364136223846793005L+1442695040888963407L; s[k]=(int)(x>>>32); }
        }
        public long nextLong(){
            int[] w = s.clone();
            for(int r=0;r<4;r++){
                w[4]^=Integer.rotateLeft(w[0]+w[12],7); w[8]^=Integer.rotateLeft(w[4]+w[0],9);
                w[12]^=Integer.rotateLeft(w[8]+w[4],13); w[0]^=Integer.rotateLeft(w[12]+w[8],18);
                w[9]^=Integer.rotateLeft(w[5]+w[1],7); w[13]^=Integer.rotateLeft(w[9]+w[5],9);
                w[1]^=Integer.rotateLeft(w[13]+w[9],13); w[5]^=Integer.rotateLeft(w[1]+w[13],18);
                w[14]^=Integer.rotateLeft(w[10]+w[6],7); w[2]^=Integer.rotateLeft(w[14]+w[10],9);
                w[6]^=Integer.rotateLeft(w[2]+w[14],13); w[10]^=Integer.rotateLeft(w[6]+w[2],18);
                w[3]^=Integer.rotateLeft(w[15]+w[11],7); w[7]^=Integer.rotateLeft(w[3]+w[15],9);
                w[11]^=Integer.rotateLeft(w[7]+w[3],13); w[15]^=Integer.rotateLeft(w[11]+w[7],18);
            }
            long r=0;
            for(int i=0;i<8;i++) r |= ((long)(w[i]+s[i]))<<(i*8);
            s[8]++;
            return r;
        }
    }

    /* 33 */ static final class Salsa20_12 implements Prng {
        private int[] s = new int[16];
        Salsa20_12(long seed){ long x=seed; for(int k=0;k<16;k++){ x=x*6364136223846793005L+1442695040888963407L; s[k]=(int)(x>>>32); } }
        public long nextLong(){
            int[] w = s.clone();
            for(int r=0;r<6;r++){
                w[4]^=Integer.rotateLeft(w[0]+w[12],7); w[8]^=Integer.rotateLeft(w[4]+w[0],9);
                w[12]^=Integer.rotateLeft(w[8]+w[4],13); w[0]^=Integer.rotateLeft(w[12]+w[8],18);
                w[9]^=Integer.rotateLeft(w[5]+w[1],7); w[13]^=Integer.rotateLeft(w[9]+w[5],9);
                w[1]^=Integer.rotateLeft(w[13]+w[9],13); w[5]^=Integer.rotateLeft(w[1]+w[13],18);
                w[14]^=Integer.rotateLeft(w[10]+w[6],7); w[2]^=Integer.rotateLeft(w[14]+w[10],9);
                w[6]^=Integer.rotateLeft(w[2]+w[14],13); w[10]^=Integer.rotateLeft(w[6]+w[2],18);
                w[3]^=Integer.rotateLeft(w[15]+w[11],7); w[7]^=Integer.rotateLeft(w[3]+w[15],9);
                w[11]^=Integer.rotateLeft(w[7]+w[3],13); w[15]^=Integer.rotateLeft(w[11]+w[7],18);
            }
            long r=0;
            for(int i=0;i<8;i++) r |= ((long)(w[i]+s[i]))<<(i*8);
            s[12]++;
            return r;
        }
    }

    /* 34 */ static final class ChaCha8 implements Prng {
        private int[] s = new int[16];
        ChaCha8(long seed){ long x=seed; for(int k=0;k<16;k++){ x=x*6364136223846793005L+1442695040888963407L; s[k]=(int)(x>>>32); } }
        private static void qr(int[] w,int a,int b,int c,int d){
            w[a]+=w[b]; w[d]^=w[a]; w[d]=Integer.rotateLeft(w[d],16);
            w[c]+=w[d]; w[b]^=w[c]; w[b]=Integer.rotateLeft(w[b],12);
            w[a]+=w[b]; w[d]^=w[a]; w[d]=Integer.rotateLeft(w[d],8);
            w[c]+=w[d]; w[b]^=w[c]; w[b]=Integer.rotateLeft(w[b],7);
        }
        public long nextLong(){
            int[] w = s.clone();
            for(int r=0;r<4;r++){
                qr(w,0,4,8,12); qr(w,1,5,9,13); qr(w,2,6,10,14); qr(w,3,7,11,15);
                qr(w,0,5,10,15); qr(w,1,6,11,12); qr(w,2,7,8,13); qr(w,3,4,9,14);
            }
            long r=0;
            for(int i=0;i<8;i++) r |= ((long)(w[i]+s[i]))<<(i*8);
            s[12]++;
            return r;
        }
    }

    /* 35 */ static final class ChaCha12 implements Prng {
        private int[] s = new int[16];
        ChaCha12(long seed){ long x=seed; for(int k=0;k<16;k++){ x=x*6364136223846793005L+1442695040888963407L; s[k]=(int)(x>>>32); } }
        public long nextLong(){
            int[] w = s.clone();
            for(int r=0;r<6;r++){
                ChaCha8.qr(w,0,4,8,12); ChaCha8.qr(w,1,5,9,13); ChaCha8.qr(w,2,6,10,14); ChaCha8.qr(w,3,7,11,15);
                ChaCha8.qr(w,0,5,10,15); ChaCha8.qr(w,1,6,11,12); ChaCha8.qr(w,2,7,8,13); ChaCha8.qr(w,3,4,9,14);
            }
            long r=0;
            for(int i=0;i<8;i++) r |= ((long)(w[i]+s[i]))<<(i*8);
            s[13]++;
            return r;
        }
    }

    /* 36 */ static final class XSalsa20 implements Prng {
        private int[] s = new int[16];
        XSalsa20(long seed){ long x=seed; for(int k=0;k<16;k++){ x=x*6364136223846793005L+1442695040888963407L; s[k]=(int)(x>>>32); } }
        public long nextLong(){
            // HSalsa20 on first 16 bytes, then Salsa20
            int[] w = s.clone();
            for(int r=0;r<10;r++){
                w[4]^=Integer.rotateLeft(w[0]+w[12],7); w[8]^=Integer.rotateLeft(w[4]+w[0],9);
                w[12]^=Integer.rotateLeft(w[8]+w[4],13); w[0]^=Integer.rotateLeft(w[12]+w[8],18);
                w[9]^=Integer.rotateLeft(w[5]+w[1],7); w[13]^=Integer.rotateLeft(w[9]+w[5],9);
                w[1]^=Integer.rotateLeft(w[13]+w[9],13); w[5]^=Integer.rotateLeft(w[1]+w[13],18);
                w[14]^=Integer.rotateLeft(w[10]+w[6],7); w[2]^=Integer.rotateLeft(w[14]+w[10],9);
                w[6]^=Integer.rotateLeft(w[2]+w[14],13); w[10]^=Integer.rotateLeft(w[6]+w[2],18);
                w[3]^=Integer.rotateLeft(w[15]+w[11],7); w[7]^=Integer.rotateLeft(w[3]+w[15],9);
                w[11]^=Integer.rotateLeft(w[7]+w[3],13); w[15]^=Integer.rotateLeft(w[11]+w[7],18);
            }
            int[] out = new int[16];
            out[0]=w[0]; out[1]=w[5]; out[2]=w[10]; out[3]=w[15];
            out[4]=w[6]; out[5]=w[7]; out[6]=w[8]; out[7]=w[9];
            out[8]=w[1]; out[9]=w[2]; out[10]=w[3]; out[11]=w[4];
            out[12]=w[11]; out[13]=w[12]; out[14]=w[13]; out[15]=w[14];
            long r=0;
            for(int i=0;i<8;i++) r |= ((long)(out[i] + s[i])) << (i*8);
            s[8]++;
            return r;
        }
    }

    /* 37 */ static final class XChaCha20 implements Prng {
        private int[] s = new int[16];
        XChaCha20(long seed){ long x=seed; for(int k=0;k<16;k++){ x=x*6364136223846793005L+1442695040888963407L; s[k]=(int)(x>>>32); } }
        public long nextLong(){
            int[] w = s.clone();
            for(int r=0;r<10;r++){
                ChaCha8.qr(w,0,4,8,12); ChaCha8.qr(w,1,5,9,13); ChaCha8.qr(w,2,6,10,14); ChaCha8.qr(w,3,7,11,15);
                ChaCha8.qr(w,0,5,10,15); ChaCha8.qr(w,1,6,11,12); ChaCha8.qr(w,2,7,8,13); ChaCha8.qr(w,3,4,9,14);
            }
            long r=0;
            for(int i=0;i<8;i++) r |= ((long)(w[i]+s[i])) << (i*8);
            s[14]++;
            return r;
        }
    }

    /* 38 */ static final class Sosemanuk implements Prng {
        private final int[] s = new int[10]; private final int[] r1 = new int[4], r2 = new int[4];
        Sosemanuk(long seed){ long x=seed; for(int k=0;k<10;k++){ x=x*6364136223846793005L+1442695040888963407L; s[k]=(int)(x>>>32); } for(int k=0;k<4;k++){ x=x*6364136223846793005L+1442695040888963407L; r1[k]=(int)(x>>>32); r2[k]=(int)(x>>>32); } }
        private static int sbox(int x){ return ((x*0x1B) ^ ((x<<3)|(x>>>5)) ^ (x*x)) & 0xFF; }
        public long nextLong(){
            long r = 0;
            for(int k=0;k<8;k++){
                int a = s[9] ^ s[3] ^ (s[0]<<8 | s[0]>>>24);
                int t = (sbox(a&0xFF) << 24) | (sbox((a>>>8)&0xFF)<<16) | (sbox((a>>>16)&0xFF)<<8) | sbox((a>>>24)&0xFF);
                r1[0] = (r1[0] + ((r1[3] ^ (r1[0]<<8 | r1[0]>>>24)) + r2[0])) & 0xFFFFFFFF;
                r2[3] = ((r2[3]<<8 | r2[3]>>>24) ^ r1[0]) + t;
                for(int i=9;i>0;i--) s[i]=s[i-1];
                s[0]=t;
                for(int i=3;i>0;i--) r1[i]=r1[i-1];
                for(int i=3;i>0;i--) r2[i]=r2[i-1];
                r |= ((long)((r1[0] ^ r2[0]) & 0xFF)) << (8*k);
            }
            return r;
        }
    }

    /* 39 */ static final class Dragon implements Prng {
        private final int[] s = new int[32]; private int idx = 0;
        Dragon(long seed){ long x=seed; for(int k=0;k<32;k++){ x=x*6364136223846793005L+1442695040888963407L; s[k]=(int)(x>>>32); } }
        public long nextLong(){
            long r=0;
            for(int k=0;k<8;k++){
                int a = s[idx];
                int b = s[(idx+1)&31];
                int c = s[(idx+2)&31];
                int d = s[(idx+3)&31];
                int f = (a ^ b) + (c ^ d);
                f = Integer.rotateLeft(f, 7) ^ Integer.rotateLeft(f, 17);
                s[idx] = f;
                r |= ((long)(f & 0xFF)) << (8*k);
                idx = (idx+1)&31;
            }
            return r;
        }
    }

    /* 40 */ static final class NLS implements Prng {
        private int[] s = new int[17]; private int i = 0;
        NLS(long seed){ long x=seed; for(int k=0;k<17;k++){ x=x*6364136223846793005L+1442695040888963407L; s[k]=(int)(x>>>32)|1; } }
        public long nextLong(){
            long r=0;
            for(int k=0;k<8;k++){
                int t = s[i] ^ s[(i+6)%17] ^ s[(i+15)%17];
                t = Integer.rotateLeft(t, 5) * 0x9E3779B9;
                s[i] = t;
                r |= ((long)(t & 0xFF)) << (8*k);
                i = (i+1)%17;
            }
            return r;
        }
    }

    /* 41 */ static final class Phelix implements Prng {
        private final int[] s = new int[8];
        Phelix(long seed){ long x=seed; for(int k=0;k<8;k++){ x=x*6364136223846793005L+1442695040888963407L; s[k]=(int)(x>>>32); } }
        public long nextLong(){
            long r=0;
            for(int k=0;k<8;k++){
                int a = s[0], b = s[3];
                s[6] = Integer.rotateLeft(s[6] + (a^b), 7);
                s[1] = Integer.rotateLeft(s[1] ^ s[6], 11) + s[2];
                s[5] = Integer.rotateLeft(s[5] + s[1], 17);
                s[2] = Integer.rotateLeft(s[2] ^ s[5], 5) + s[7];
                r |= ((long)(s[0] ^ s[4]) & 0xFF) << (8*k);
            }
            return r;
        }
    }

    /* 42 */ static final class PolarBear implements Prng {
        private final int[] s = new int[8]; private int ctr = 0;
        PolarBear(long seed){ long x=seed; for(int k=0;k<8;k++){ x=x*6364136223846793005L+1442695040888963407L; s[k]=(int)(x>>>32); } }
        public long nextLong(){
            long r=0;
            for(int k=0;k<8;k++){
                int t = s[0] + s[3] + ctr++;
                s[5] = Integer.rotateLeft(s[5] ^ t, 13) + s[7];
                s[2] = Integer.rotateLeft(s[2] + s[5], 17);
                r |= ((long)(s[2] ^ s[0]) & 0xFF) << (8*k);
                for(int i=7;i>0;i--) s[i]=s[i-1];
                s[0]=t;
            }
            return r;
        }
    }

    /* 43 */ static final class Mir1 implements Prng {
        private final int[] s = new int[8]; private int ctr = 0;
        Mir1(long seed){ long x=seed; for(int k=0;k<8;k++){ x=x*6364136223846793005L+1442695040888963407L; s[k]=(int)(x>>>32); } }
        public long nextLong(){
            long r=0;
            for(int k=0;k<8;k++){
                int a = s[0], b = s[4];
                int f = (a ^ b) + (a & b);
                f = Integer.rotateLeft(f, 11);
                s[0] = f;
                s[4] = Integer.rotateLeft(s[4] + ctr++, 7);
                r |= ((long)(s[0] ^ s[4]) & 0xFF) << (8*k);
            }
            return r;
        }
    }

    /* 44 */ static final class MUGI implements Prng {
        private final int[] s = new int[16]; private int i = 0;
        MUGI(long seed){ long x=seed; for(int k=0;k<16;k++){ x=x*6364136223846793005L+1442695040888963407L; s[k]=(int)(x>>>32); } }
        public long nextLong(){
            long r=0;
            for(int k=0;k<8;k++){
                int t = s[i] ^ s[(i+7)&15] ^ Integer.rotateLeft(s[(i+13)&15], 11);
                s[i] = t;
                r |= ((long)(t & 0xFF)) << (8*k);
                i = (i+1)&15;
            }
            return r;
        }
    }

    /* 45 */ static final class Enocoro128 implements Prng {
        private final int[] b = new int[32]; private final int[] s = new int[3];
        Enocoro128(long seed){ long x=seed; for(int k=0;k<32;k++){ x=x*6364136223846793005L+1442695040888963407L; b[k]=(int)(x>>>32); } for(int k=0;k<3;k++){ x=x*6364136223846793005L+1442695040888963407L; s[k]=(int)(x>>>32); } }
        public long nextLong(){
            long r=0;
            for(int k=0;k<8;k++){
                int t = b[0] ^ b[2] ^ b[15] ^ b[29];
                s[0] = (s[0] + s[1]) & 0xFFFFFFFF;
                s[1] = (s[1] ^ s[2]) + s[0];
                s[2] = s[2] + s[1];
                for(int i=31;i>0;i--) b[i]=b[i-1];
                b[0]=t;
                r |= ((long)((s[0] ^ s[1] ^ s[2]) & 0xFF)) << (8*k);
            }
            return r;
        }
    }

    /* 46 */ static final class HC128b implements Prng {
        private final int[] p = new int[512], q = new int[512]; private int cnt = 0;
        HC128b(long seed){ long x=seed; for(int k=0;k<512;k++){ x=x*6364136223846793005L+1442695040888963407L; p[k]=(int)(x>>>32); q[k]=(int)(x>>>32); } }
        public long nextLong(){
            long r=0;
            for(int k=0;k<8;k++){
                int j = cnt & 511;
                int v;
                if(cnt < 512){ v = p[(j-3)&511] ^ p[(j-10)&511] ^ p[(j-511)&511]; p[j] = v + q[(j-12)&511]; }
                else { v = q[(j-3)&511] ^ q[(j-10)&511] ^ q[(j-511)&511]; q[j] = v + p[(j-12)&511]; }
                cnt = (cnt+1)&1023;
                r |= ((long)(v & 0xFF)) << (8*k);
            }
            return r;
        }
    }

    /* 47 */ static final class HC256b implements Prng {
        private final int[] p = new int[1024], q = new int[1024]; private int cnt = 0;
        HC256b(long seed){ long x=seed; for(int k=0;k<1024;k++){ x=x*6364136223846793005L+1442695040888963407L; p[k]=(int)(x>>>32); q[k]=(int)(x>>>32); } }
        public long nextLong(){
            long r=0;
            for(int k=0;k<8;k++){
                int j = cnt & 1023;
                int v;
                if(cnt < 1024){ v = p[(j-3)&1023] ^ p[(j-10)&1023] ^ p[(j-1023)&1023]; p[j] = v + q[(j-12)&1023]; }
                else { v = q[(j-3)&1023] ^ q[(j-10)&1023] ^ q[(j-1023)&1023]; q[j] = v + p[(j-12)&1023]; }
                cnt = (cnt+1)&2047;
                r |= ((long)(v & 0xFF)) << (8*k);
            }
            return r;
        }
    }

    /* 48 */ static final class E0 implements Prng {
        private final int[] lfsr1 = new int[25], lfsr2 = new int[31], lfsr3 = new int[33], lfsr4 = new int[39];
        E0(long seed){ long x=seed; for(int k=0;k<25;k++){ x=x*6364136223846793005L+1442695040888963407L; lfsr1[k]=(int)(x>>>32)&1; } for(int k=0;k<31;k++){ x=x*6364136223846793005L+1442695040888963407L; lfsr2[k]=(int)(x>>>32)&1; } for(int k=0;k<33;k++){ x=x*6364136223846793005L+1442695040888963407L; lfsr3[k]=(int)(x>>>32)&1; } for(int k=0;k<39;k++){ x=x*6364136223846793005L+1442695040888963407L; lfsr4[k]=(int)(x>>>32)&1; } }
        private static int lfsrStep(int[] r, int[] taps){
            int out = r[0];
            int f = 0;
            for(int t : taps) f ^= r[t];
            System.arraycopy(r, 1, r, 0, r.length-1);
            r[r.length-1] = f;
            return out;
        }
        public long nextLong(){
            long out=0;
            for(int k=0;k<64;k++){
                int y1 = lfsr1[0];
                int y2 = lfsr2[0];
                int y3 = lfsr3[0];
                int y4 = lfsr4[0];
                int z = y1 ^ y2 ^ y3 ^ y4;
                out |= ((long)z) << k;
                int s = (y1&1) + (y2&1) + (y3&1) + (y4&1);
                if(s >= 2){
                    lfsrStep(lfsr1, new int[]{3, 12});
                    lfsrStep(lfsr2, new int[]{4, 7});
                    lfsrStep(lfsr3, new int[]{5, 17});
                    lfsrStep(lfsr4, new int[]{6, 26});
                } else {
                    lfsrStep(lfsr2, new int[]{4, 7});
                    lfsrStep(lfsr3, new int[]{5, 17});
                }
            }
            return out;
        }
    }

    /* 49 */ static final class Pike implements Prng {
        private final int[] tab = new int[512]; private int idx = 0, b = 0, c = 0;
        Pike(long seed){ long x=seed; for(int k=0;k<512;k++){ x=x*6364136223846793005L+1442695040888963407L; tab[k]=(int)(x>>>32); } }
        public long nextLong(){
            long out=0;
            for(int k=0;k<8;k++){
                b = (b+1)&255;
                c = (tab[b] + tab[(b+1)&511])&255;
                int t = tab[(tab[b]+tab[c])&511];
                tab[(b+1)&511] = t ^ tab[b];
                out |= ((long)(tab[c] ^ tab[b]) & 0xFF) << (8*k);
                idx++;
            }
            return out;
        }
    }

    /* 50 */ static final class Turing implements Prng {
        private final int[] tab = new int[512]; private int idx = 0;
        Turing(long seed){ long x=seed; for(int k=0;k<512;k++){ x=x*6364136223846793005L+1442695040888963407L; tab[k]=(int)(x>>>32); } }
        public long nextLong(){
            long out=0;
            for(int k=0;k<8;k++){
                int p = tab[idx & 511] & 511;
                int q = tab[(idx+1) & 511] & 511;
                int t = tab[p] + tab[q];
                tab[idx & 511] = t ^ Integer.rotateLeft(tab[q], 7);
                out |= ((long)(t & 0xFF)) << (8*k);
                idx++;
            }
            return out;
        }
    }

    /* 51 */ static final class Leviathan implements Prng {
        private final int[] lfsr = new int[12];
        Leviathan(long seed){ long x=seed; for(int k=0;k<12;k++){ x=x*6364136223846793005L+1442695040888963407L; lfsr[k]=(int)(x>>>32); } }
        public long nextLong(){
            long out=0;
            for(int k=0;k<8;k++){
                int t = lfsr[0] ^ lfsr[5] ^ lfsr[11];
                int fb = (lfsr[0] & 1) ^ (lfsr[3] & 1) ^ (lfsr[7] & 1) ^ (lfsr[11] & 1);
                for(int i=11;i>0;i--) lfsr[i]=lfsr[i-1];
                lfsr[0] = Integer.rotateLeft(t, 5) ^ fb;
                out |= ((long)(t & 0xFF)) << (8*k);
            }
            return out;
        }
    }

    /* 52 */ static final class Fubuki implements Prng {
        private final int[] s = new int[16]; private int idx = 0;
        Fubuki(long seed){ long x=seed; for(int k=0;k<16;k++){ x=x*6364136223846793005L+1442695040888963407L; s[k]=(int)(x>>>32); } }
        public long nextLong(){
            long out=0;
            for(int k=0;k<8;k++){
                int a = s[idx & 15];
                int b = s[(idx+5)&15];
                int c = Integer.rotateLeft(a, 7) ^ b;
                s[(idx+5)&15] = c;
                out |= ((long)(c & 0xFF)) << (8*k);
                idx++;
            }
            return out;
        }
    }

    /* 53 */ static final class K2 implements Prng {
        private final int[] s = new int[24]; private int idx = 0;
        K2(long seed){ long x=seed; for(int k=0;k<24;k++){ x=x*6364136223846793005L+1442695040888963407L; s[k]=(int)(x>>>32); } }
        public long nextLong(){
            long out=0;
            for(int k=0;k<8;k++){
                int a = s[idx % 24];
                int b = s[(idx+4)%24];
                int c = s[(idx+11)%24];
                int f = Integer.rotateLeft(a, 7) ^ Integer.rotateLeft(b, 13) ^ c;
                s[idx % 24] = f + Integer.rotateLeft(a, 3);
                out |= ((long)(f & 0xFF)) << (8*k);
                idx++;
            }
            return out;
        }
    }

    /* 54 */ static final class WG7 implements Prng {
        private final int[] lfsr = new int[32]; private int wg = 0;
        WG7(long seed){ long x=seed; for(int k=0;k<32;k++){ x=x*6364136223846793005L+1442695040888963407L; lfsr[k]=(int)(x>>>32); } }
        private static int wgt(int x){ return ((x*3) ^ (x>>>3) ^ Integer.rotateLeft(x, 11)) & 0xFFFFFFFF; }
        public long nextLong(){
            long out=0;
            for(int k=0;k<8;k++){
                int t = lfsr[0] ^ lfsr[3] ^ lfsr[13] ^ lfsr[31];
                int fb = wgt(lfsr[0] ^ lfsr[5] ^ lfsr[15]) & 1;
                for(int i=31;i>0;i--) lfsr[i]=lfsr[i-1];
                lfsr[0] = Integer.rotateLeft(t, 3) ^ fb;
                wg = wgt(wg + lfsr[0]);
                out |= ((long)(wg & 0xFF)) << (8*k);
            }
            return out;
        }
    }

    /* 55 */ static final class Helix implements Prng {
        private final int[] z = new int[5];
        Helix(long seed){ long x=seed; for(int k=0;k<5;k++){ x=x*6364136223846793005L+1442695040888963407L; z[k]=(int)(x>>>32); } }
        public long nextLong(){
            long out=0;
            for(int k=0;k<8;k++){
                int a = z[0] ^ z[2];
                int b = z[1] + z[3];
                z[4] = Integer.rotateLeft(z[4] ^ a, 7) + b;
                z[0] = Integer.rotateLeft(z[0] + z[4], 13);
                z[3] = Integer.rotateLeft(z[3] ^ z[0], 17);
                out |= ((long)((z[0] ^ z[4]) & 0xFF)) << (8*k);
            }
            return out;
        }
    }

    /* 56 */ static final class Panama implements Prng {
        private final int[] s = new int[17]; private final int[] b = new int[32];
        Panama(long seed){ long x=seed; for(int k=0;k<17;k++){ x=x*6364136223846793005L+1442695040888963407L; s[k]=(int)(x>>>32); } for(int k=0;k<32;k++){ x=x*6364136223846793005L+1442695040888963407L; b[k]=(int)(x>>>32); } }
        public long nextLong(){
            long out=0;
            for(int k=0;k<8;k++){
                int q = s[0] ^ b[0];
                b[0] = Integer.rotateLeft(b[0] + s[1], 5) ^ q;
                for(int i=16;i>0;i--) s[i]=s[i-1];
                s[0] = Integer.rotateLeft(q, 7);
                out |= ((long)(q & 0xFF)) << (8*k);
            }
            return out;
        }
    }

    /* 57 */ static final class WAKE implements Prng {
        private final int[] tab = new int[256]; private int idx = 0, r = 0;
        WAKE(long seed){ long x=seed; for(int k=0;k<256;k++){ x=x*6364136223846793005L+1442695040888963407L; tab[k]=(int)(x>>>32); } }
        public long nextLong(){
            long out=0;
            for(int k=0;k<8;k++){
                r = tab[(r ^ tab[idx & 255]) & 255];
                tab[idx & 255] = r;
                out |= ((long)(r & 0xFF)) << (8*k);
                idx++;
            }
            return out;
        }
    }

    /* 58 */ static final class SEAL implements Prng {
        private final int[] t = new int[512]; private int idx = 0; private int a=0,b=0,c=0,d=0;
        SEAL(long seed){ long x=seed; for(int k=0;k<512;k++){ x=x*6364136223846793005L+1442695040888963407L; t[k]=(int)(x>>>32); } }
        public long nextLong(){
            long out=0;
            for(int k=0;k<8;k++){
                int n = idx++;
                a = (a + t[(n+0)&511]) & 0xFFFFFFFF;
                b = (b + t[(n+1)&511]) & 0xFFFFFFFF;
                c = (c + t[(n+2)&511]) & 0xFFFFFFFF;
                d = (d + t[(n+3)&511]) & 0xFFFFFFFF;
                int y = Integer.rotateLeft(a, 9) ^ Integer.rotateLeft(b, 13) ^ Integer.rotateLeft(c, 19) ^ d;
                out |= ((long)(y & 0xFF)) << (8*k);
            }
            return out;
        }
    }

    /* 59 */ static final class SCREAM implements Prng {
        private int[] s = new int[16];
        SCREAM(long seed){ long x=seed; for(int k=0;k<16;k++){ x=x*6364136223846793005L+1442695040888963407L; s[k]=(int)(x>>>32); } }
        public long nextLong(){
            int[] w = s.clone();
            for(int r=0;r<8;r++){
                ChaCha8.qr(w,0,4,8,12); ChaCha8.qr(w,1,5,9,13); ChaCha8.qr(w,2,6,10,14); ChaCha8.qr(w,3,7,11,15);
                ChaCha8.qr(w,0,5,10,15); ChaCha8.qr(w,1,6,11,12); ChaCha8.qr(w,2,7,8,13); ChaCha8.qr(w,3,4,9,14);
            }
            long out=0;
            for(int i=0;i<8;i++) out |= ((long)(w[i]+s[i])) << (i*8);
            s[15]++;
            return out;
        }
    }

    /* 60 */ static final class Rabbit2 implements Prng {
        private final int[] x = new int[8], c = new int[8];
        Rabbit2(long seed){ long s = seed; for(int k=0;k<8;k++){ x[k]=(int)(s=splitmix64(s)); c[k]=(int)(s=splitmix64(s)); } }
        public long nextLong(){
            int[] g = new int[8];
            for(int i=0;i<8;i++){
                long t = (x[i]&0xFFFFL)*(x[(i+1)&7]&0xFFFFL) + (c[i]&0xFFFFFFFFL);
                g[i] = (int)t;
                c[(i+1)&7] = (int)(t>>>16);
            }
            long r = 0;
            for(int i=0;i<8;i++) r ^= ((long)g[i]) << (i*8);
            for(int i=0;i<8;i++) x[i] ^= g[i];
            return r;
        }
    }

    /* 91 */ static final class NistCtrDrbg implements Prng {
        private byte[] v = new byte[16];
        private long ctr = 0;
        NistCtrDrbg(long seed){ long x=seed; for(int i=0;i<16;i++){ x=splitmix64(x); v[i]=(byte)(x>>>56); } }
        public long nextLong(){
            long r=0;
            for(int i=0;i<8;i++){
                for(int j=v.length-1;j>=0;j--){ v[j]++; if(v[j]!=0) break; }
                r |= ((long)(v[i%16]&0xFF)) << (8*i);
            }
            ctr++;
            for(int i=0;i<16;i++) v[i] ^= (byte)(ctr >>> (8*(i%8)));
            return r;
        }
    }

    /* 92 */ static final class NistHmacDrbg implements Prng {
        private long k, vv;
        NistHmacDrbg(long seed){ k = seed ^ 0x9E3779B97F4A7C15L; vv = splitmix64(seed); }
        public long nextLong(){
            long x = vv;
            for(int i=0;i<4;i++){
                x = (x ^ k) * 0x9E3779B97F4A7C15L;
                k = Long.rotateLeft(k, 13) ^ x;
            }
            vv = x;
            return x ^ k;
        }
    }

    /* 93 */ static final class NistHashDrbg implements Prng {
        private long s; private int reseed = 0;
        NistHashDrbg(long seed){ s = seed; }
        public long nextLong(){
            s = (s ^ (s>>>30)) * 0xBF58476D1CE4E5B9L;
            s = (s ^ (s>>>27)) * 0x94D049BB133111EBL;
            reseed++;
            if((reseed & 0xFFFF) == 0) s ^= 0x9E3779B97F4A7C15L;
            return s ^ (s>>>31);
        }
    }

    /* 94 */ static final class DualEcDrbg implements Prng {
        private long s; private final long p = 0xFFFFFFFFFFFFFFC5L;
        DualEcDrbg(long seed){ s = (seed|1) % p; }
        public long nextLong(){
            s = (s*s + 1) % p;
            long r = (s * 0x9E3779B97F4A7C15L) % p;
            s = (r*r + 1) % p;
            return s ^ r;
        }
    }

    /* 95 */ static final class AnsiX917 implements Prng {
        private final byte[] d = new byte[8]; private long ctr = 0;
        AnsiX917(long seed){ long x=seed; for(int i=0;i<8;i++){ x=splitmix64(x); d[i]=(byte)(x>>>56); } }
        public long nextLong(){
            long r = 0;
            for(int i=0;i<8;i++){
                ctr++;
                d[i] = (byte)((d[i] + ctr + i) & 0xFF);
                r |= ((long)(d[i]&0xFF)) << (8*i);
            }
            return r;
        }
    }

    /* 96 */ static final class AnsiX931 implements Prng {
        private byte[] v = new byte[8], r = new byte[8];
        AnsiX931(long seed){
            long x=seed;
            for(int i=0;i<8;i++){ x=splitmix64(x); v[i]=(byte)(x>>>56); r[i]=(byte)(x>>>32); }
        }
        public long nextLong(){
            long out = 0;
            for(int i=0;i<8;i++){
                v[i] = (byte)(v[i] ^ r[i] ^ (i*3));
                r[i] = (byte)((r[i]*0x1B) ^ v[i]);
                out |= ((long)(r[i]&0xFF)) << (8*i);
            }
            return out;
        }
    }

    /* 97 */ static final class Fortuna2 implements Prng {
        private final long[] pool = new long[32]; private int idx = 0;
        Fortuna2(long seed){ long x=seed; for(int i=0;i<32;i++) pool[i] = x = splitmix64(x); }
        public long nextLong(){
            long r = pool[idx & 31];
            pool[idx & 31] = splitmix64(pool[idx & 31] + idx);
            idx++;
            return r ^ Long.rotateLeft(r, 17) ^ Long.rotateLeft(r, 41);
        }
    }

    /* 98 */ static final class Yarrow160 implements Prng {
        private long ctr, key; private final long[] st = new long[4];
        Yarrow160(long seed){
            key = seed;
            long x = seed;
            for(int i=0;i<4;i++) st[i] = x = splitmix64(x);
            ctr = 0;
        }
        public long nextLong(){
            ctr++;
            long x = st[0] ^ st[1] ^ st[2] ^ st[3] ^ ctr ^ key;
            st[(int)(ctr & 3)] = splitmix64(x);
            return x;
        }
    }

    /* 99 */ static final class SP800_90A implements Prng {
        private long seed, ctr;
        SP800_90A(long s){ seed = s; ctr = 0; }
        public long nextLong(){
            ctr++;
            long x = seed + ctr * 0x9E3779B97F4A7C15L;
            x = (x ^ (x>>>30)) * 0xBF58476D1CE4E5B9L;
            x = (x ^ (x>>>27)) * 0x94D049BB133111EBL;
            return x ^ (x>>>31);
        }
    }

    /* 100 */ static final class MicaliSchnorr implements Prng {
        private long x; private final long n;
        MicaliSchnorr(long seed){
            n = 0x7FFFFFFFFFFFFFFFL - 1;
            x = (seed | 1) % n;
        }
        public long nextLong(){
            x = (x * x * x) % n;
            return x ^ Long.rotateLeft(x, 29);
        }
    }

    // =================================================
    // ==========  BATCH 3: Distribution/Sampling  ====
    // =================================================

    /* 101 */ static final class GaussianBoxMuller implements Prng {
        private long s; GaussianBoxMuller(long k){ s = k|1; }
        public long nextLong(){
            s = splitmix64(s + 0x9E3779B97F4A7C15L);
            double u1 = u(s) + 1e-300;
            double u2 = u(splitmix64(s));
            double z = Math.sqrt(-2*Math.log(u1)) * Math.cos(6.283185307179586*u2);
            return Double.doubleToLongBits(z) ^ (s * 0xBF58476D1CE4E5B9L);
        }
    }

    /* 102 */ static final class GaussianZiggurat implements Prng {
        private final double[] x = new double[128], y = new double[128];
        private long s;
        GaussianZiggurat(long k){
            s = k|1;
            x[0]=3.442619855899; y[0]=0.0;
            for(int i=1;i<128;i++){ x[i]=Math.sqrt(-2*Math.log(y[i-1]+Math.exp(-0.5*x[i-1]*x[i-1])/(x[i-1]+1e-300))); y[i]=Math.exp(-0.5*x[i]*x[i]); }
        }
        public long nextLong(){
            s = splitmix64(s + 0x9E3779B97F4A7C15L);
            int i = (int)((s>>>56)&127);
            double uu = u(s)*x[i+1];
            if(uu < x[i]) return Double.doubleToLongBits(uu) ^ (s*0xBF58476D1CE4E5B9L);
            double z = uu / x[i];
            return Double.doubleToLongBits(z) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 103 */ static final class GaussianRatioOfUniforms implements Prng {
        private long s; GaussianRatioOfUniforms(long k){ s = k|1; }
        public long nextLong(){
            while(true){
                s = splitmix64(s + 0x9E3779B97F4A7C15L);
                double uu = u(s)*2-1, vv = u(splitmix64(s))*2-1;
                double q = uu*uu + vv*vv;
                if(q > 1) continue;
                double z = 4.0 * uu / (q + 1e-300);
                return Double.doubleToLongBits(z) ^ (s*0xBF58476D1CE4E5B9L);
            }
        }
    }

    /* 104 */ static final class ExponentialS implements Prng {
        private long s; ExponentialS(long k){ s=k|1; }
        public long nextLong(){ s = splitmix64(s + 0x9E3779B97F4A7C15L); double e = -Math.log(1.0-u(s)+1e-300); return Double.doubleToLongBits(e) ^ (s*0xBF58476D1CE4E5B9L); }
    }

    /* 105 */ static final class PoissonKnuth implements Prng {
        private final double lambda; private long s;
        PoissonKnuth(long k){ s=k|1; lambda = 4.0; }
        public long nextLong(){
            s = splitmix64(s + 0x9E3779B97F4A7C15L);
            double L = Math.exp(-lambda); int k=0; double p=1.0;
            do { k++; p *= u(splitmix64(s+k)); } while(p > L);
            return ((long)(k-1)) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 106 */ static final class PoissonRejection implements Prng {
        private final double lambda; private long s;
        PoissonRejection(long k){ s=k|1; lambda = 10.0; }
        public long nextLong(){
            s = splitmix64(s + 0x9E3779B97F4A7C15L);
            double c = 0.767 - 3.36/lambda;
            double beta = Math.PI / Math.sqrt(3*lambda);
            double alpha = beta * lambda;
            double kconst = Math.log(c) - lambda - Math.log(beta);
            while(true){
                double uu = u(splitmix64(s++));
                double x = (alpha - Math.log((1.0-uu)/(uu+1e-300))) / beta;
                int n = (int)Math.floor(x + 0.5);
                if(n < 0) continue;
                double vv = u(splitmix64(s++));
                double y = alpha - beta*x;
                double lhs = y + Math.log(vv/(1.0+Math.exp(y))/(1.0+Math.exp(y)) + 1e-300);
                double rhs = kconst + n*Math.log(lambda+1e-300) - logFact(n);
                if(lhs <= rhs) return ((long)n) ^ (s*0xBF58476D1CE4E5B9L);
            }
        }
        private static double logFact(int n){ double r=0; for(int i=2;i<=n;i++) r+=Math.log(i); return r; }
    }

    /* 107 */ static final class GammaMarsaglia implements Prng {
        private final double shape, scale; private long s;
        GammaMarsaglia(long k){ s=k|1; shape=2.0; scale=1.5; }
        public long nextLong(){
            s = splitmix64(s + 0x9E3779B97F4A7C15L);
            double a = shape, d = a - 1.0/3.0, c = 1.0/Math.sqrt(9.0*d);
            while(true){
                double x = g(splitmix64(s++)); double vv = 1.0 + c*x;
                if(vv <= 0) continue;
                vv = vv*vv*vv;
                double uu = u(splitmix64(s++));
                if(uu < 1.0 - 0.0331*x*x*x*x || Math.log(uu+1e-300) < 0.5*x*x + d*(1.0 - vv + Math.log(vv+1e-300)))
                    return Double.doubleToLongBits(d*vv*scale) ^ (s*0xBF58476D1CE4E5B9L);
            }
        }
    }

    /* 108 */ static final class BetaJohnk implements Prng {
        private final double a, b; private long s;
        BetaJohnk(long k){ s=k|1; a=2.0; b=5.0; }
        public long nextLong(){
            s = splitmix64(s + 0x9E3779B97F4A7C15L);
            double x = Math.pow(u(splitmix64(s++))+1e-300, 1.0/a);
            double y = Math.pow(u(splitmix64(s++))+1e-300, 1.0/b);
            double beta = x / (x + y + 1e-300);
            return Double.doubleToLongBits(beta) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 109 */ static final class BetaCheng implements Prng {
        private final double a, b; private long s;
        BetaCheng(long k){ s=k|1; a=2.0; b=5.0; }
        public long nextLong(){
            s = splitmix64(s + 0x9E3779B97F4A7C15L);
            double alpha = a + b;
            double beta = Math.sqrt((alpha - 2.0) / (2.0*a*b - alpha));
            double gamma = a + 1.0/beta;
            while(true){
                double u1 = u(splitmix64(s++));
                double u2 = u(splitmix64(s++));
                double vv = beta * Math.log(u1/(1.0-u1+1e-300));
                double w = a * Math.exp(vv);
                double z = u1*u1*u2;
                double r = gamma*vv - 1.3862943611198906;
                double ss = a + r - w;
                if(ss + 2.6094379124341003 > 5.0*z) return Double.doubleToLongBits(w/(b+w)) ^ (s*0xBF58476D1CE4E5B9L);
                if(ss >= Math.log(z)) return Double.doubleToLongBits(w/(b+w)) ^ (s*0xBF58476D1CE4E5B9L);
            }
        }
    }

    /* 110 */ static final class BinomialBTPE implements Prng {
        private final int n; private final double p; private long s;
        BinomialBTPE(long k){ s=k|1; n=20; p=0.3; }
        public long nextLong(){
            s = splitmix64(s + 0x9E3779B97F4A7C15L);
            double q = 1-p;
            double np = n*p, nq = n*q;
            double fm = Math.floor(np + p);
            double m = fm;
            double p1 = Math.floor(2.195*Math.sqrt(n*p*q) - 4.6*q) + 0.5;
            double xm = m + 0.5;
            double xl = xm - p1;
            double xr = xm + p1;
            double c = 0.134 + 20.5/(15.3 + m);
            double al = (fm - xl)/(fm - xl*p);
            double xll = al * (1.0 + 0.5*al);
            al = (xr - fm)/(xr*q);
            double xlr = al * (1.0 + 0.5*al);
            while(true){
                double uu = u(splitmix64(s++)) * (xll + xlr + c);
                double vv = u(splitmix64(s++));
                if(uu <= xll){
                    int kk = (int)(xm - p1*vv + 0.5);
                    return ((long)kk) ^ (s*0xBF58476D1CE4E5B9L);
                }
                if(uu <= xll + xlr){
                    int kk = (int)(xm + p1*vv + 0.5);
                    return ((long)kk) ^ (s*0xBF58476D1CE4E5B9L);
                }
                double y = uu - (xll + xlr);
                if(y <= c){
                    int kk = (int)(xl + y/c + 0.5);
                    return ((long)kk) ^ (s*0xBF58476D1CE4E5B9L);
                }
            }
        }
    }

    /* 111 */ static final class BinomialInversion implements Prng {
        private final int n; private final double p; private long s;
        BinomialInversion(long k){ s=k|1; n=20; p=0.3; }
        public long nextLong(){
            s = splitmix64(s + 0x9E3779B97F4A7C15L);
            int c = 0;
            for(int i=0;i<n;i++) if(u(splitmix64(s+i)) < p) c++;
            return ((long)c) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 112 */ static final class GeometricS implements Prng {
        private final double p; private long s;
        GeometricS(long k){ s=k|1; p=0.2; }
        public long nextLong(){ s=splitmix64(s+0x9E3779B97F4A7C15L); return ((long)(Math.floor(Math.log(1.0-u(s)+1e-300)/Math.log(1.0-p)))) ^ (s*0xBF58476D1CE4E5B9L); }
    }

    /* 113 */ static final class ParetoS implements Prng {
        private final double scale, alpha; private long s;
        ParetoS(long k){ s=k|1; scale=1.0; alpha=3.0; }
        public long nextLong(){ s=splitmix64(s+0x9E3779B97F4A7C15L); double x = scale/Math.pow(1.0-u(s)+1e-300, 1.0/alpha); return Double.doubleToLongBits(x) ^ (s*0xBF58476D1CE4E5B9L); }
    }

    /* 114 */ static final class CauchyS implements Prng {
        private long s; CauchyS(long k){ s=k|1; }
        public long nextLong(){ s=splitmix64(s+0x9E3779B97F4A7C15L); double x=Math.tan(3.141592653589793*(u(s)-0.5)); return Double.doubleToLongBits(x) ^ (s*0xBF58476D1CE4E5B9L); }
    }

    /* 115 */ static final class LogisticS implements Prng {
        private long s; LogisticS(long k){ s=k|1; }
        public long nextLong(){ s=splitmix64(s+0x9E3779B97F4A7C15L); double x=Math.log(u(s)/(1.0-u(s)+1e-300)+1e-300); return Double.doubleToLongBits(x) ^ (s*0xBF58476D1CE4E5B9L); }
    }

    /* 116 */ static final class LaplaceS implements Prng {
        private long s; LaplaceS(long k){ s=k|1; }
        public long nextLong(){ s=splitmix64(s+0x9E3779B97F4A7C15L); double uu=u(s)-0.5; double x=-Math.signum(uu)*Math.log(1.0-2.0*Math.abs(uu)+1e-300); return Double.doubleToLongBits(x) ^ (s*0xBF58476D1CE4E5B9L); }
    }

    /* 117 */ static final class WeibullS implements Prng {
        private final double scale, shape; private long s;
        WeibullS(long k){ s=k|1; scale=1.0; shape=2.0; }
        public long nextLong(){ s=splitmix64(s+0x9E3779B97F4A7C15L); double x = scale*Math.pow(-Math.log(1.0-u(s)+1e-300), 1.0/shape); return Double.doubleToLongBits(x) ^ (s*0xBF58476D1CE4E5B9L); }
    }

    /* 118 */ static final class TriangularS implements Prng {
        private long s; TriangularS(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double uu=u(s);
            double x = uu < 0.5 ? Math.sqrt(uu/2) : 1.0 - Math.sqrt((1.0-uu)/2);
            return Double.doubleToLongBits(x) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 119 */ static final class GumbelS implements Prng {
        private long s; GumbelS(long k){ s=k|1; }
        public long nextLong(){ s=splitmix64(s+0x9E3779B97F4A7C15L); double x=-Math.log(-Math.log(u(s)+1e-300)+1e-300); return Double.doubleToLongBits(x) ^ (s*0xBF58476D1CE4E5B9L); }
    }

    /* 120 */ static final class LogNormalS implements Prng {
        private long s; LogNormalS(long k){ s=k|1; }
        public long nextLong(){ s=splitmix64(s+0x9E3779B97F4A7C15L); double x=Math.exp(g(s)); return Double.doubleToLongBits(x) ^ (s*0xBF58476D1CE4E5B9L); }
    }

    /* 121 */ static final class StudentTS implements Prng {
        private final int df; private long s;
        StudentTS(long k){ s=k|1; df=5; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double z = g(splitmix64(s++));
            double chi = 0; for(int i=0;i<df;i++) chi += g(splitmix64(s+++i))*g(splitmix64(s+++i));
            double t = z / Math.sqrt(chi/df + 1e-300);
            return Double.doubleToLongBits(t) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 122 */ static final class ChiSquareS implements Prng {
        private final int df; private long s;
        ChiSquareS(long k){ s=k|1; df=3; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double chi=0;
            for(int i=0;i<df;i++){ double z=g(splitmix64(s+++i)); chi += z*z; }
            return Double.doubleToLongBits(chi) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 123 */ static final class FDistributionS implements Prng {
        private long s; FDistributionS(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double c1=0; for(int i=0;i<5;i++){ double z=g(splitmix64(s++)); c1 += z*z; }
            double c2=0; for(int i=0;i<10;i++){ double z=g(splitmix64(s++)); c2 += z*z; }
            double f = (c1/5.0)/(c2/10.0);
            return Double.doubleToLongBits(f) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 124 */ static final class DirichletS implements Prng {
        private long s; DirichletS(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double x1 = Math.pow(u(splitmix64(s++)), 1.0/2.0);
            double x2 = Math.pow(u(splitmix64(s++)), 1.0/3.0);
            double x3 = Math.pow(u(splitmix64(s++)), 1.0/5.0);
            double sum = x1+x2+x3;
            return Double.doubleToLongBits(x1/sum) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 125 */ static final class VonMisesS implements Prng {
        private long s; VonMisesS(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double kappa = 4.0;
            double a = 1.0 + Math.sqrt(1 + 4*kappa*kappa);
            double b = (a - Math.sqrt(2*a)) / (2*kappa);
            double r = (1 + b*b) / (2*b);
            while(true){
                double u1 = u(splitmix64(s++));
                double z = Math.cos(Math.PI * u1);
                double ff = (1 + r*z) / (r + z);
                double c = kappa*(r - ff);
                double u2 = u(splitmix64(s++));
                if(c*(2.0-c) - u2 > 0 || Math.log(c/u2) + 1 - c >= 0)
                    return Double.doubleToLongBits(ff) ^ (s*0xBF58476D1CE4E5B9L);
            }
        }
    }

    /* 126 */ static final class RayleighS implements Prng {
        private long s; RayleighS(long k){ s=k|1; }
        public long nextLong(){ s=splitmix64(s+0x9E3779B97F4A7C15L); double x=Math.sqrt(-2*Math.log(1.0-u(s)+1e-300)); return Double.doubleToLongBits(x) ^ (s*0xBF58476D1CE4E5B9L); }
    }

    /* 127 */ static final class MaxwellS implements Prng {
        private long s; MaxwellS(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double x1=g(splitmix64(s++)), x2=g(splitmix64(s++)), x3=g(splitmix64(s++));
            double v=Math.sqrt(x1*x1+x2*x2+x3*x3);
            return Double.doubleToLongBits(v) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 128 */ static final class WaldS implements Prng {
        private long s; WaldS(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double mu=1.0, lambda=1.0;
            double v=g(splitmix64(s++)); double y=v*v;
            double x = mu + (mu*mu*y)/(2*lambda) - (mu/(2*lambda))*Math.sqrt(4*mu*lambda*y + mu*mu*y*y);
            double uu = u(splitmix64(s++));
            if(uu <= mu/(mu+x)) return Double.doubleToLongBits(x) ^ (s*0xBF58476D1CE4E5B9L);
            return Double.doubleToLongBits(mu*mu/x) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 129 */ static final class NakagamiS implements Prng {
        private long s; NakagamiS(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double m=2.0, omega=1.0;
            double gsum=0; int n=(int)(m*2);
            for(int i=0;i<n;i++){ double z=g(splitmix64(s+++i)); gsum += z*z; }
            double x = Math.sqrt(omega/(2.0*m))*gsum;
            return Double.doubleToLongBits(x) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 130 */ static final class ZipfS implements Prng {
        private long s; ZipfS(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double q=2.0; int n=1000;
            double sum=0; for(int i=1;i<=n;i++) sum += Math.pow(i, -q);
            double uu = u(s)*sum;
            double acc=0;
            for(int i=1;i<=n;i++){ acc += Math.pow(i,-q); if(acc >= uu) return ((long)i) ^ (s*0xBF58476D1CE4E5B9L); }
            return 1 ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 131 */ static final class RandomizedQuickselect implements Prng {
        private int[] arr; private long s; private int step = 0;
        RandomizedQuickselect(long k){ s=k|1; arr = new int[64]; for(int i=0;i<64;i++) arr[i]=i*3+1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            int lo=0, hi=arr.length-1, target=arr.length/2;
            while(lo<hi){
                int pivot = arr[lo + (int)(u(splitmix64(s++)) * (hi-lo+1))];
                int i=lo, j=hi;
                while(i<=j){
                    while(arr[i]<pivot) i++;
                    while(arr[j]>pivot) j--;
                    if(i<=j){ int t=arr[i]; arr[i]=arr[j]; arr[j]=t; i++; j--; }
                }
                if(target<=j) hi=j; else if(target>=i) lo=i; else break;
            }
            step++;
            return ((long)arr[target]) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 132 */ static final class RandomizedQuicksort implements Prng {
        private int[] arr; private long s; private int pass = 0;
        RandomizedQuicksort(long k){ s=k|1; arr = new int[64]; for(int i=0;i<64;i++) arr[i]=(int)(splitmix64(k+i)>>>56); }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            for(int p=0;p<64;p++){
                int r = p + (int)(u(splitmix64(s++)) * (64-p));
                int t=arr[p]; arr[p]=arr[r]; arr[r]=t;
            }
            java.util.Arrays.sort(arr);
            pass++;
            return ((long)arr[pass & 63]) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 133 */ static final class KargerMinCut implements Prng {
        private int[] parent; private long s;
        KargerMinCut(long k){ s=k|1; parent = new int[16]; for(int i=0;i<16;i++) parent[i]=i; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            for(int i=0;i<16;i++) parent[i]=i;
            int comps = 16;
            while(comps > 2){
                int a = (int)(u(splitmix64(s++)) * 16);
                int b = (int)(u(splitmix64(s++)) * 16);
                int ra = find(a), rb = find(b);
                if(ra != rb){ parent[ra] = rb; comps--; }
            }
            return ((long)comps) ^ (s*0xBF58476D1CE4E5B9L);
        }
        private int find(int x){ while(parent[x]!=x){ parent[x]=parent[parent[x]]; x=parent[x]; } return x; }
    }

    /* 134 */ static final class MillerRabin implements Prng {
        private long s; private int bitLen = 61;
        MillerRabin(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long n = (1L << bitLen) | (s & ((1L << bitLen) - 1));
            if(n % 2 == 0) n++;
            long d = n-1; int r = 0;
            while((d & 1) == 0){ d >>= 1; r++; }
            long a = 2 + (long)(u(splitmix64(s++)) * (n-3));
            long x = modpow(a, d, n);
            if(x == 1 || x == n-1) return n;
            for(int i=0;i<r-1;i++){
                x = (x*x) % n;
                if(x == n-1) return n;
            }
            return -1 ^ (s*0xBF58476D1CE4E5B9L);
        }
        private static long modpow(long b, long e, long m){
            long r = 1; b %= m;
            while(e > 0){ if((e&1)==1) r = (r*b)%m; b=(b*b)%m; e >>= 1; }
            return r;
        }
    }

    /* 135 */ static final class SolovayStrassen implements Prng {
        private long s; SolovayStrassen(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long n = 0x1FFFFFFFFFFFFFFFL ^ (s>>>3);
            if(n % 2 == 0) n++;
            long a = 2 + (long)(u(splitmix64(s++)) * (n-3));
            long x = jacobi(a, n);
            long y = modpow(a, (n-1)/2, n);
            return (x == y ? n : -1) ^ (s*0xBF58476D1CE4E5B9L);
        }
        private static long jacobi(long a, long n){
            long r = 1;
            while(a != 0){
                while((a&1)==0){ a>>=1; long m=n&7; if(m==3||m==5) r=-r; }
                long t=a; a=n; n=t;
                if((a&3)==3 && (n&3)==3) r=-r;
                a %= n;
            }
            return n==1 ? r : 0;
        }
        private static long modpow(long b, long e, long m){ long r=1; b%=m; while(e>0){ if((e&1)==1) r=(r*b)%m; b=(b*b)%m; e>>=1; } return r; }
    }

    /* 136 */ static final class SchwartzZippel implements Prng {
        private long s; SchwartzZippel(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long p = 1000000007L;
            long r = 0;
            for(int i=0;i<20;i++){
                long x = (long)(u(splitmix64(s++)) * p);
                long v = 0;
                for(int j=0;j<10;j++) v = (v*x + j) % p;
                r = (r + v) % p;
            }
            return r ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 137 */ static final class Freivalds implements Prng {
        private long s; Freivalds(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            int n = 32;
            int[] x = new int[n];
            for(int i=0;i<n;i++) x[i] = (u(splitmix64(s++)) < 0.5 ? 0 : 1);
            long h = 0;
            for(int i=0;i<n;i++) h = h*31 + x[i];
            return h ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 138 */ static final class RabinKarp implements Prng {
        private long s; RabinKarp(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long base = 256, mod = 1000000009L;
            long h = 0;
            for(int i=0;i<16;i++){
                h = (h*base + (s>>>56)) % mod;
                s = splitmix64(s);
            }
            return h ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 139 */ static final class PollardRho implements Prng {
        private long s; PollardRho(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long n = 0x1FFFFFFFFL | (s & 0xFFFFFFFFL);
            if(n % 2 == 0) n++;
            long x = 2, y = 2, d = 1;
            long c = 1 + (long)(u(splitmix64(s++)) * (n-1));
            while(d == 1){
                x = (x*x + c) % n;
                y = (y*y + c) % n; y = (y*y + c) % n;
                d = gcd(Math.abs(x-y), n);
            }
            return d ^ (s*0xBF58476D1CE4E5B9L);
        }
        private static long gcd(long a, long b){ while(b!=0){ long t=a%b; a=b; b=t; } return a; }
    }

    /* 140 */ static final class PollardP1 implements Prng {
        private long s; PollardP1(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long n = 0xFFFFFFFFFFFFFL | (s & 0xFFFFFFFFFFFFL);
            if((n & 1) == 0) n++;
            long a = 2, B = 100;
            for(long i=2;i<=B;i++){
                a = modpow(a, i, n);
                long d = gcd(a-1, n);
                if(d > 1 && d < n) return d ^ (s*0xBF58476D1CE4E5B9L);
            }
            return n ^ (s*0xBF58476D1CE4E5B9L);
        }
        private static long modpow(long b, long e, long m){ long r=1; b%=m; while(e>0){ if((e&1)==1) r=(r*b)%m; b=(b*b)%m; e>>=1; } return r; }
        private static long gcd(long a, long b){ while(b!=0){ long t=a%b; a=b; b=t; } return a; }
    }

    /* 141 */ static final class WalkSAT implements Prng {
        private final int n = 32; private boolean[] x; private long s;
        WalkSAT(long k){ s=k|1; x = new boolean[n]; for(int i=0;i<n;i++) x[i] = (splitmix64(k+i) & 1) != 0; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            int idx = (int)(u(splitmix64(s++)) * n);
            x[idx] = !x[idx];
            long h = 0;
            for(int i=0;i<n;i++) if(x[i]) h |= 1L<<i;
            return h ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 142 */ static final class GSAT implements Prng {
        private final int n = 32; private boolean[] x; private long s;
        GSAT(long k){ s=k|1; x = new boolean[n]; for(int i=0;i<n;i++) x[i] = (splitmix64(k+i) & 1) != 0; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            int best = -1; int bestScore = Integer.MIN_VALUE;
            for(int i=0;i<n;i++){
                x[i] = !x[i];
                int sc = score();
                if(sc > bestScore){ bestScore = sc; best = i; }
                x[i] = !x[i];
            }
            if(best >= 0) x[best] = !x[best];
            long h = 0;
            for(int i=0;i<n;i++) if(x[i]) h |= 1L<<i;
            return h ^ (s*0xBF58476D1CE4E5B9L);
        }
        private int score(){ int c=0; for(int i=0;i<n;i++) if(x[i] == ((i&1)==0)) c++; return c; }
    }

    /* 143 */ static final class SimulatedAnnealing implements Prng {
        private double[] x; private long s; private double T = 100.0;
        SimulatedAnnealing(long k){ s=k|1; x = new double[16]; for(int i=0;i<16;i++) x[i] = u(splitmix64(k+i)); }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            int i = (int)(u(splitmix64(s++)) * 16);
            double old = x[i];
            double next = old + (u(splitmix64(s++)) - 0.5) * 2 * T;
            double dE = f(next) - f(old);
            if(dE < 0 || u(splitmix64(s++)) < Math.exp(-dE/T)) x[i] = next;
            T *= 0.999;
            return Double.doubleToLongBits(x[i]) ^ (s*0xBF58476D1CE4E5B9L);
        }
        private static double f(double v){ return v*v; }
    }

    /* 144 */ static final class RandomizedRounding implements Prng {
        private long s; RandomizedRounding(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double[] frac = new double[8];
            for(int i=0;i<8;i++) frac[i] = u(splitmix64(s++));
            long r = 0;
            for(int i=0;i<8;i++) if(u(splitmix64(s++)) < frac[i]) r |= 1L<<i;
            return r ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 145 */ static final class RandomizedGreedy implements Prng {
        private long s; RandomizedGreedy(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            boolean[] taken = new boolean[32];
            for(int i=0;i<16;i++){
                int best = -1;
                for(int j=0;j<32;j++){
                    if(!taken[j] && (best < 0 || u(splitmix64(s++)) < 0.5)) best = j;
                }
                if(best >= 0) taken[best] = true;
            }
            long r = 0;
            for(int i=0;i<32;i++) if(taken[i]) r |= 1L<<i;
            return r ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 146 */ static final class RandomizedLoadBalancing implements Prng {
        private final long[] bins = new long[16]; private long s;
        RandomizedLoadBalancing(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            int i = (int)(u(splitmix64(s++)) * 16);
            bins[i]++;
            return bins[i] ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 147 */ static final class ReservoirSampling implements Prng {
        private final int[] res = new int[16]; private int n = 0; private long s;
        ReservoirSampling(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            int item = (int)(splitmix64(s) >>> 40);
            n++;
            if(n <= 16) res[n-1] = item;
            else {
                int j = (int)(u(splitmix64(s++)) * n);
                if(j < 16) res[j] = item;
            }
            return ((long)res[n % 16]) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 148 */ static final class ImportanceSampling implements Prng {
        private long s; ImportanceSampling(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double sum = 0, wsum = 0;
            for(int i=0;i<16;i++){
                double x = g(splitmix64(s++));
                double w = Math.exp(-x*x/4);
                sum += x*w; wsum += w;
            }
            return Double.doubleToLongBits(sum/(wsum+1e-300)) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 149 */ static final class BootstrapSampling implements Prng {
        private final double[] data = new double[64]; private long s;
        BootstrapSampling(long k){ s=k|1; for(int i=0;i<64;i++) data[i] = g(splitmix64(k+i)); }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double sum = 0;
            for(int i=0;i<64;i++){
                int idx = (int)(u(splitmix64(s++)) * 64);
                sum += data[idx];
            }
            return Double.doubleToLongBits(sum/64) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 150 */ static final class JackknifeSampling implements Prng {
        private final double[] data = new double[32]; private long s;
        JackknifeSampling(long k){ s=k|1; for(int i=0;i<32;i++) data[i] = g(splitmix64(k+i)); }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            int leave = (int)(u(splitmix64(s++)) * 32);
            double sum = 0;
            for(int i=0;i<32;i++) if(i != leave) sum += data[i];
            return Double.doubleToLongBits(sum/31) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 151 */ static final class SMOTESampling implements Prng {
        private final double[][] data = new double[32][2]; private long s;
        SMOTESampling(long k){ s=k|1; for(int i=0;i<32;i++){ data[i][0]=g(splitmix64(k+i)); data[i][1]=g(splitmix64(k+i+100)); } }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            int i = (int)(u(splitmix64(s++)) * 32);
            int j = (int)(u(splitmix64(s++)) * 32);
            double l = u(splitmix64(s++));
            double x = data[i][0] + l*(data[j][0]-data[i][0]);
            return Double.doubleToLongBits(x) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 152 */ static final class ADASYNSampling implements Prng {
        private long s; ADASYNSampling(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double x = g(splitmix64(s++));
            double noise = g(splitmix64(s++)) * 0.1;
            double y = x + noise;
            return Double.doubleToLongBits(y) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 153 */ static final class MCMCSampling implements Prng {
        private double x = 0; private long s;
        MCMCSampling(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double prop = x + g(splitmix64(s++)) * 0.5;
            double lr = Math.exp(-0.5*(prop*prop - x*x));
            if(u(splitmix64(s++)) < lr) x = prop;
            return Double.doubleToLongBits(x) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 154 */ static final class MetropolisHastings implements Prng {
        private double x = 0; private long s;
        MetropolisHastings(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double prop = x + g(splitmix64(s++)) * 0.3;
            double lr = Math.exp(-0.5*(prop*prop - x*x));
            if(Math.log(u(splitmix64(s++))+1e-300) < lr) x = prop;
            return Double.doubleToLongBits(x) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 155 */ static final class GibbsSampling implements Prng {
        private final double[] x = new double[4]; private long s;
        GibbsSampling(long k){ s=k|1; for(int i=0;i<4;i++) x[i] = g(splitmix64(k+i)); }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            for(int i=0;i<4;i++){
                double mean = 0;
                for(int j=0;j<4;j++) if(j!=i) mean += x[j];
                x[i] = mean/3.0 + g(splitmix64(s++)) * 0.5;
            }
            return Double.doubleToLongBits(x[0]) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 156 */ static final class HamiltonianMC implements Prng {
        private double x = 0, p = 0; private long s;
        HamiltonianMC(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double eps = 0.1; int L = 10;
            double x0 = x, p0 = p;
            p += eps * (-x) / 2;
            for(int i=0;i<L;i++){
                x += eps * p;
                if(i < L-1) p += eps * (-x);
            }
            p += eps * (-x) / 2;
            double H0 = 0.5*x0*x0 + 0.5*p0*p0;
            double H1 = 0.5*x*x + 0.5*p*p;
            if(u(splitmix64(s++)) < Math.exp(H0-H1)) { }
            else { x = x0; p = p0; }
            return Double.doubleToLongBits(x) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 157 */ static final class SliceSampling implements Prng {
        private double x = 0; private long s;
        SliceSampling(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double y = Math.exp(-0.5*x*x) * u(splitmix64(s++));
            double lo = x - 3, hi = x + 3;
            while(lo*lo < -2*Math.log(y+1e-300) && hi*hi < -2*Math.log(y+1e-300)){
                double nw = hi - lo;
                lo -= nw; hi += nw;
            }
            for(int i=0;i<32;i++){
                double prop = lo + u(splitmix64(s++)) * (hi - lo);
                if(Math.exp(-0.5*prop*prop) > y){ x = prop; break; }
                if(prop < x) lo = prop; else hi = prop;
            }
            return Double.doubleToLongBits(x) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 158 */ static final class RejectionSampling implements Prng {
        private long s; RejectionSampling(long k){ s=k|1; }
        public long nextLong(){
            while(true){
                s=splitmix64(s+0x9E3779B97F4A7C15L);
                double x = u(splitmix64(s++)) * 2 - 1;
                double y = u(splitmix64(s++));
                if(y < Math.exp(-x*x)) return Double.doubleToLongBits(x) ^ (s*0xBF58476D1CE4E5B9L);
            }
        }
    }

    /* 159 */ static final class AdaptiveRejection implements Prng {
        private double x = 0; private long s;
        AdaptiveRejection(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double lo = x - 1, hi = x + 1;
            for(int i=0;i<8;i++){
                double prop = lo + u(splitmix64(s++)) * (hi - lo);
                if(Math.exp(-0.5*prop*prop) > u(splitmix64(s++))){ x = prop; break; }
                if(prop < x) lo = prop; else hi = prop;
            }
            return Double.doubleToLongBits(x) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 160 */ static final class SequentialMonteCarlo implements Prng {
        private final double[] particles = new double[32]; private final double[] weights = new double[32]; private long s;
        SequentialMonteCarlo(long k){ s=k|1; for(int i=0;i<32;i++){ particles[i]=g(splitmix64(k+i)); weights[i]=1.0/32; } }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double[] nw = new double[32]; double sum = 0;
            for(int i=0;i<32;i++){ nw[i] = weights[i] * Math.exp(-0.5*particles[i]*particles[i]); sum += nw[i]; }
            double est = 0;
            for(int i=0;i<32;i++) est += particles[i] * nw[i] / (sum + 1e-300);
            return Double.doubleToLongBits(est) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 161 */ static final class BloomFilter implements Prng {
        private final long[] bits = new long[64]; private long s;
        BloomFilter(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long key = s;
            int h1 = (int)(key ^ (key>>>32));
            int h2 = (int)((key*0x9E3779B9L) ^ (key>>>17));
            for(int i=0;i<3;i++){
                int idx = Math.abs((h1 + i*h2) % 4096);
                bits[idx>>>6] |= 1L << (idx & 63);
            }
            return bits[(int)(key & 63)] ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 162 */ static final class CountingBloomFilter implements Prng {
        private final int[] counts = new int[256]; private long s;
        CountingBloomFilter(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long key = s;
            int h1 = (int)(key ^ (key>>>32));
            int h2 = (int)((key*0x9E3779B9L) ^ (key>>>17));
            for(int i=0;i<3;i++){
                int idx = Math.abs((h1 + i*h2) % 256);
                counts[idx]++;
            }
            return counts[Math.abs(h1) % 256] ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 163 */ static final class CuckooFilter implements Prng {
        private final long[] slots = new long[256]; private long s;
        CuckooFilter(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long key = s;
            int i1 = Math.abs((int)key) % 256;
            int i2 = (i1 ^ Math.abs((int)(key>>>32))) % 256;
            if(slots[i1] == 0) slots[i1] = key;
            else if(slots[i2] == 0) slots[i2] = key;
            else { slots[i1] = key; }
            return slots[i1] ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 164 */ static final class MinHash implements Prng {
        private final int[] sig = new int[128]; private long s;
        MinHash(long k){ s=k|1; java.util.Arrays.fill(sig, Integer.MAX_VALUE); }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long key = s;
            for(int i=0;i<128;i++){
                int h = (int)(splitmix64(key + i) >>> 32);
                if(h < sig[i]) sig[i] = h;
            }
            return sig[(int)(key & 127)] ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 165 */ static final class WeightedMinHash implements Prng {
        private long s; WeightedMinHash(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long best = Long.MAX_VALUE; double bestT = Double.MAX_VALUE;
            for(int i=0;i<128;i++){
                double w = u(splitmix64(s++));
                double t = Math.log(u(splitmix64(s++))+1e-300)/w;
                if(t < bestT){ bestT = t; best = splitmix64(s+i); }
            }
            return best ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 166 */ static final class BBitMinHash implements Prng {
        private long s; BBitMinHash(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            int b = 2;
            long r = 0;
            for(int i=0;i<16;i++){
                int h = (int)(splitmix64(s+i) >>> (64-b));
                r = (r << b) | h;
            }
            return r ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 167 */ static final class SimHash implements Prng {
        private final int[] v = new int[64]; private long s;
        SimHash(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long key = s;
            for(int i=0;i<64;i++){
                int bit = (int)((key>>>i)&1);
                v[i] += bit == 1 ? 1 : -1;
            }
            long r = 0;
            for(int i=0;i<64;i++) if(v[i] > 0) r |= 1L<<i;
            return r ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 168 */ static final class OnePermutationHashing implements Prng {
        private long s; OnePermutationHashing(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long[] bins = new long[8];
            java.util.Arrays.fill(bins, Long.MAX_VALUE);
            for(int i=0;i<64;i++){
                long h = splitmix64(s+i);
                int bin = (int)(h & 7);
                if(h < bins[bin]) bins[bin] = h;
            }
            long r = 0;
            for(int i=0;i<8;i++) r ^= bins[i];
            return r ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 169 */ static final class DensifiedMinHash implements Prng {
        private final long[] bins = new long[8]; private long s;
        DensifiedMinHash(long k){ s=k|1; java.util.Arrays.fill(bins, Long.MAX_VALUE); }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long h = splitmix64(s);
            int bin = (int)(h & 7);
            if(h < bins[bin]) bins[bin] = h;
            long r = 0;
            for(int i=0;i<8;i++){
                if(bins[i] == Long.MAX_VALUE) r ^= 0x9E3779B97F4A7C15L * (i+1);
                else r ^= bins[i];
            }
            return r ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 170 */ static final class SuperMinHash implements Prng {
        private final long[] slots = new long[16]; private final int[] counts = new int[16]; private long s;
        SuperMinHash(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long h = splitmix64(s);
            int bin = (int)(h & 15);
            counts[bin]++;
            if(counts[bin] == 1) slots[bin] = h;
            else if(splitmix64(h) % counts[bin] == 0) slots[bin] = h;
            return slots[bin] ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 171 */ static final class HyperLogLog implements Prng {
        private final int[] regs = new int[256]; private long s;
        HyperLogLog(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long h = splitmix64(s);
            int idx = (int)(h & 255);
            int rho = Long.numberOfLeadingZeros(h >>> 8) + 1;
            if(rho > regs[idx]) regs[idx] = rho;
            return regs[idx] ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 172 */ static final class HyperLogLogPP implements Prng {
        private final int[] regs = new int[256]; private long s;
        HyperLogLogPP(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long h = splitmix64(s);
            int idx = (int)(h & 255);
            int rho = Long.numberOfLeadingZeros(h >>> 8) + 1;
            if(rho > regs[idx]) regs[idx] = rho;
            return regs[idx] ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 173 */ static final class KMV implements Prng {
        private final long[] heap = new long[32]; private int size = 0; private long s;
        KMV(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long h = splitmix64(s);
            if(size < 32){ heap[size++] = h; }
            else {
                int mx = 0;
                for(int i=1;i<32;i++) if(heap[i] > heap[mx]) mx = i;
                if(h < heap[mx]) heap[mx] = h;
            }
            long m = Long.MAX_VALUE;
            for(int i=0;i<size;i++) if(heap[i] < m) m = heap[i];
            return m ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 174 */ static final class ThetaSketch implements Prng {
        private final long[] samples = new long[32]; private int size = 0; private long theta = Long.MAX_VALUE; private long s;
        ThetaSketch(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long h = splitmix64(s) & Long.MAX_VALUE;
            if(h < theta){
                if(size < 32){ samples[size++] = h; }
                else {
                    int mx = 0;
                    for(int i=1;i<32;i++) if(samples[i] > samples[mx]) mx = i;
                    if(h < samples[mx]) samples[mx] = h;
                }
                if(size == 32){
                    long mx = 0;
                    for(int i=0;i<32;i++) if(samples[i] > mx) mx = samples[i];
                    theta = mx;
                }
            }
            return theta ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 175 */ static final class CountMin implements Prng {
        private final int[][] table = new int[4][256]; private long s;
        CountMin(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long key = s;
            int mn = Integer.MAX_VALUE;
            for(int i=0;i<4;i++){
                int idx = Math.abs((int)(splitmix64(key + i) & 255));
                table[i][idx]++;
                if(table[i][idx] < mn) mn = table[i][idx];
            }
            return mn ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 176 */ static final class CountSketch implements Prng {
        private final int[][] table = new int[4][256]; private long s;
        CountSketch(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long key = s;
            long result = 0;
            for(int i=0;i<4;i++){
                long h = splitmix64(key + i);
                int idx = Math.abs((int)(h & 255));
                int sign = ((h>>>32)&1)==0 ? 1 : -1;
                table[i][idx] += sign;
                result += table[i][idx] * sign;
            }
            return result ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 177 */ static final class AMSSketch implements Prng {
        private final int[][] table = new int[4][256]; private long s;
        AMSSketch(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long key = s;
            for(int i=0;i<4;i++){
                int idx = Math.abs((int)(splitmix64(key + i*17) & 255));
                table[i][idx]++;
            }
            return table[0][Math.abs((int)key & 255)] ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 178 */ static final class TugOfWar implements Prng {
        private final long[][] table = new long[4][256]; private long s;
        TugOfWar(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long key = s;
            long result = 0;
            for(int i=0;i<4;i++){
                long h = splitmix64(key + i);
                int idx = Math.abs((int)(h & 255));
                long sign = ((h>>>32)&1)==0 ? 1 : -1;
                table[i][idx] += sign;
                result += table[i][idx] * sign;
            }
            return result ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 179 */ static final class RandomProjection implements Prng {
        private final int k = 64; private long s;
        RandomProjection(long kk){ s=kk|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double sum = 0;
            for(int i=0;i<k;i++) sum += g(splitmix64(s++)) * g(splitmix64(s++));
            return Double.doubleToLongBits(sum/Math.sqrt(k)) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 180 */ static final class AchlioptasProjection implements Prng {
        private final int k = 64; private long s;
        AchlioptasProjection(long kk){ s=kk|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long r = 0;
            for(int i=0;i<k;i++){
                double uu = u(splitmix64(s++));
                int val;
                if(uu < 1.0/6) val = -1;
                else if(uu < 5.0/6) val = 0;
                else val = 1;
                r = (r << 2) | (val + 1);
            }
            return r ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 181 */ static final class SparseRandomProjection implements Prng {
        private final int k = 64; private final double density = 1.0/3; private long s;
        SparseRandomProjection(long kk){ s=kk|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long r = 0;
            for(int i=0;i<k;i++){
                double uu = u(splitmix64(s++));
                if(uu < density){
                    r = (r << 1) | (uu < density/2 ? 1 : 0);
                } else {
                    r = (r << 1);
                }
            }
            return r ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 182 */ static final class JLTransform implements Prng {
        private long s; JLTransform(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            int d = 128;
            double sum = 0;
            for(int i=0;i<d;i++) sum += g(splitmix64(s++));
            return Double.doubleToLongBits(sum / Math.sqrt(d)) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 183 */ static final class RandomFourierFeatures implements Prng {
        private long s; RandomFourierFeatures(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double w = g(splitmix64(s++));
            double x = g(splitmix64(s++));
            double f = Math.cos(w*x) * Math.sqrt(2.0);
            return Double.doubleToLongBits(f) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 184 */ static final class NystromSketch implements Prng {
        private long s; NystromSketch(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            int m = 32;
            double sum = 0;
            for(int i=0;i<m;i++) sum += Math.exp(-0.5*g(splitmix64(s++))*g(splitmix64(s++)));
            return Double.doubleToLongBits(sum/m) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 185 */ static final class TensorSketch implements Prng {
        private long s; TensorSketch(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long a = splitmix64(s++), b = splitmix64(s++), c = splitmix64(s++);
            return (a ^ b ^ c ^ (a*b) ^ (b*c)) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 186 */ static final class FeatureHashing implements Prng {
        private long s; FeatureHashing(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long h = splitmix64(s);
            int idx = Math.abs((int)h & 255);
            return idx ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 187 */ static final class LSH implements Prng {
        private long s; LSH(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long h = 0;
            for(int i=0;i<8;i++) h = (h<<8) | (Math.abs((int)(splitmix64(s+i) & 255)));
            return h ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 188 */ static final class PStableLSH implements Prng {
        private long s; PStableLSH(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double dot = 0;
            for(int i=0;i<16;i++) dot += g(splitmix64(s++)) * u(splitmix64(s++));
            return Double.doubleToLongBits(Math.floor(dot)) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 189 */ static final class LSHForest implements Prng {
        private long s; LSHForest(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long r = 0;
            for(int i=0;i<8;i++){
                long h = splitmix64(s+i);
                r = (r<<8) | (h & 0xFF);
            }
            return r ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 190 */ static final class RandomBitsForest implements Prng {
        private long s; RandomBitsForest(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long r = 0;
            for(int i=0;i<16;i++) r = (r<<4) | (splitmix64(s+i) & 0xF);
            return r ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 191 */ static final class IsolationForest implements Prng {
        private long s; IsolationForest(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double depth = 0;
            for(int i=0;i<8;i++){
                double x = g(splitmix64(s++));
                if(x > 2.0 || x < -2.0) { depth += 1; break; }
                depth += 1;
            }
            return Double.doubleToLongBits(depth) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 192 */ static final class RandomSubspace implements Prng {
        private long s; RandomSubspace(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long r = 0;
            for(int i=0;i<32;i++) if(u(splitmix64(s++)) < 0.5) r |= 1L<<i;
            return r ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 193 */ static final class BaggingSampler implements Prng {
        private long s; BaggingSampler(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            int[] sample = new int[32];
            for(int i=0;i<32;i++) sample[i] = (int)(u(splitmix64(s++)) * 32);
            long r = 0;
            for(int v : sample) r = r*31 + v;
            return r ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 194 */ static final class RandomPatches implements Prng {
        private long s; RandomPatches(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long r = 0;
            for(int i=0;i<32;i++) if(u(splitmix64(s++)) < 0.4) r |= 1L<<i;
            return r ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 195 */ static final class ExtremelyRandomizedTrees implements Prng {
        private long s; ExtremelyRandomizedTrees(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long treeHash = 0;
            for(int d=0;d<8;d++){
                int feat = (int)(u(splitmix64(s++)) * 32);
                int cut = (int)(u(splitmix64(s++)) * 100);
                treeHash = treeHash*31 + feat*100 + cut;
            }
            return treeHash ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 196 */ static final class RandomForestSplit implements Prng {
        private long s; RandomForestSplit(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long r = 0;
            for(int t=0;t<8;t++){
                int mtry = (int)(u(splitmix64(s++)) * 32);
                r = (r<<8) | (mtry & 0xFF);
            }
            return r ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 197 */ static final class ReservoirCounter implements Prng {
        private final long[] res = new long[64]; private int n = 0; private long s;
        ReservoirCounter(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long item = splitmix64(s);
            n++;
            if(n <= 64) res[n-1] = item;
            else {
                int j = (int)(u(splitmix64(s++)) * n);
                if(j < 64) res[j] = item;
            }
            return res[(int)(n & 63)] ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 198 */ static final class SketchMedian implements Prng {
        private final long[] buf = new long[32]; private int n = 0; private long s;
        SketchMedian(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            long v = splitmix64(s);
            if(n < 32) buf[n++] = v;
            else {
                int idx = (int)(u(splitmix64(s++)) * n);
                if(idx < 32) buf[idx] = v;
            }
            long[] copy = java.util.Arrays.copyOf(buf, n);
            java.util.Arrays.sort(copy);
            return copy[n/2] ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 199 */ static final class RandomQuantile implements Prng {
        private final double[] buf = new double[64]; private int n = 0; private long s;
        RandomQuantile(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double v = g(s);
            if(n < 64) buf[n++] = v;
            else {
                int idx = (int)(u(splitmix64(s++)) * n);
                if(idx < 64) buf[idx] = v;
            }
            double[] copy = java.util.Arrays.copyOf(buf, n);
            java.util.Arrays.sort(copy);
            int q = (int)(u(splitmix64(s++)) * n);
            return Double.doubleToLongBits(copy[q]) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    /* 200 */ static final class TDigest implements Prng {
        private final double[] centers = new double[64]; private final double[] weights = new double[64];
        private int n = 0; private long s;
        TDigest(long k){ s=k|1; }
        public long nextLong(){
            s=splitmix64(s+0x9E3779B97F4A7C15L);
            double v = g(s);
            if(n < 64){ centers[n] = v; weights[n] = 1; n++; }
            else {
                int closest = 0;
                for(int i=1;i<n;i++) if(Math.abs(centers[i]-v) < Math.abs(centers[closest]-v)) closest = i;
                double w = weights[closest];
                centers[closest] = (centers[closest]*w + v)/(w+1);
                weights[closest] = w+1;
            }
            int idx = (int)(u(splitmix64(s++)) * n);
            return Double.doubleToLongBits(centers[idx]) ^ (s*0xBF58476D1CE4E5B9L);
        }
    }

    static final class AesCmacPrf implements Prng {
        private final byte[] key;
        private final byte[] k1;
        private final byte[] k2;
        private long ctr = 0;

        AesCmacPrf(long seed) {
            this.key = longToBytes(seed, seed * 0x9E3779B97F4A7C15L);
            byte[] L = aesEcbEncrypt(key, new byte[16]);
            this.k1 = cmacDouble(L);
            this.k2 = cmacDouble(k1);
        }

        private static byte[] cmacDouble(byte[] in) {
            byte[] out = new byte[in.length];
            int carry = 0;
            for (int i = in.length - 1; i >= 0; i--) {
                int v = ((in[i] & 0xFF) << 1) | carry;
                out[i] = (byte)v;
                carry = (v >>> 8) & 1;
            }
            if (carry != 0) out[in.length - 1] ^= (byte)0x87;
            return out;
        }

        public long nextLong() {
            ctr++;
            byte[] msg = longToBytes(ctr, 0);
            // 单块消息：CBC-MAC = AES_K(M XOR K1)
            byte[] block = new byte[16];
            for (int i = 0; i < 16; i++) block[i] = (byte)(msg[i] ^ k1[i]);
            byte[] mac = aesEcbEncrypt(key, block);
            return beLong(mac, 0);
        }
    }

    static final class AesGmacPrf implements Prng {
        private final byte[] key;
        private final byte[] H;
        private long ctr = 0;

        AesGmacPrf(long seed) {
            this.key = longToBytes(seed, seed * 0x9E3779B97F4A7C15L);
            this.H = aesEcbEncrypt(key, new byte[16]);
        }

        public long nextLong() {
            ctr++;
            byte[] aad = longToBytes(ctr, 0);

            // GHASH(H, A, C)，A 长度 16 字节（128 位），C 为空
            byte[] y = new byte[16];
            for (int i = 0; i < 16; i++) y[i] ^= aad[i];
            y = gf128Mul(y, H);

            byte[] lenBlock = new byte[16];
            long bitLen = 128L; // len(A) 以位为单位
            for (int i = 0; i < 8; i++) lenBlock[i] = (byte)(bitLen >>> (56 - i * 8));
            for (int i = 0; i < 16; i++) y[i] ^= lenBlock[i];
            y = gf128Mul(y, H);

            // J0 = 0^96 || 1
            byte[] j0 = new byte[16];
            j0[15] = 1;
            byte[] ek = aesEcbEncrypt(key, j0);

            byte[] tag = new byte[16];
            for (int i = 0; i < 16; i++) tag[i] = (byte)(ek[i] ^ y[i]);
            return beLong(tag, 0);
        }
    }

    static final class AesPrf implements Prng {
        private final byte[] key;
        private long ctr = 0;

        AesPrf(long seed) {
            this.key = longToBytes(seed, seed * 0x9E3779B97F4A7C15L);
        }

        public long nextLong() {
            ctr++;
            byte[] block = new byte[16];
            for (int i = 0; i < 8; i++) block[15 - i] = (byte)(ctr >>> (i * 8));
            byte[] enc = aesEcbEncrypt(key, block);
            return beLong(enc, 0);
        }
    }

    static final class AesXtsPrf implements Prng {
        private final byte[] key1;
        private final byte[] key2;
        private long ctr = 0;

        AesXtsPrf(long seed) {
            this.key1 = longToBytes(seed, seed * 0x9E3779B97F4A7C15L);
            this.key2 = longToBytes(seed ^ 0xA5A5A5A5A5A5A5A5L,
                                    seed + 0x5A5A5A5A5A5A5A5AL);
        }

        public long nextLong() {
            ctr++;
            byte[] tweak = new byte[16];
            for (int i = 0; i < 8; i++) tweak[15 - i] = (byte)(ctr >>> (i * 8));
            byte[] T = aesEcbEncrypt(key2, tweak);

            // 明文 P = 0，则 P XOR T = T
            byte[] block = T.clone();
            byte[] c = aesEcbEncrypt(key1, block);
            for (int i = 0; i < 16; i++) c[i] ^= T[i];
            return beLong(c, 0);
        }
    }

    static final class HmacSha256Prf implements Prng {
        private final byte[] key;
        private long ctr = 0;

        HmacSha256Prf(long seed) {
            this.key = longToBytes(seed, seed * 0x9E3779B97F4A7C15L);
        }

        public long nextLong() {
            ctr++;
            byte[] mac = hmacSha256(key, longToBytes(ctr, 0));
            return beLong(mac, 0);
        }
    }

    static final class HmacSha512Prf implements Prng {
        private final byte[] key;
        private long ctr = 0;

        HmacSha512Prf(long seed) {
            this.key = longToBytes(seed, seed * 0x9E3779B97F4A7C15L);
        }

        public long nextLong() {
            ctr++;
            byte[] mac = hmacSha512(key, longToBytes(ctr, 0));
            return beLong(mac, 0);
        }
    }

    static final class HmacBlake2Prf implements Prng {
        private final byte[] key;
        private long ctr = 0;

        HmacBlake2Prf(long seed) {
            this.key = longToBytes(seed, seed * 0x9E3779B97F4A7C15L);
        }

        public long nextLong() {
            ctr++;
            byte[] mac = hmacBlake2b(key, longToBytes(ctr, 0));
            return beLong(mac, 0);
        }
    }

    static final class HmacSm3Prf implements Prng {
        private final byte[] key;
        private long ctr = 0;

        HmacSm3Prf(long seed) {
            this.key = longToBytes(seed, seed * 0x9E3779B97F4A7C15L);
        }

        public long nextLong() {
            ctr++;
            byte[] mac = hmacSm3(key, longToBytes(ctr, 0));
            return beLong(mac, 0);
        }
    }

    static final class Kmac128 implements Prng {
        private final byte[] key;
        private long ctr = 0;

        Kmac128(long seed) {
            this.key = longToBytes(seed, seed * 0x9E3779B97F4A7C15L);
        }

        public long nextLong() {
            ctr++;
            byte[] out = kmac128(key, longToBytes(ctr, 0), 16);
            return beLong(out, 0);
        }
    }

    static final class Kmac256 implements Prng {
        private final byte[] key;
        private long ctr = 0;

        Kmac256(long seed) {
            this.key = longToBytes(seed, seed * 0x9E3779B97F4A7C15L);
        }

        public long nextLong() {
            ctr++;
            byte[] out = kmac256(key, longToBytes(ctr, 0), 16);
            return beLong(out, 0);
        }
    }

    // =================================================
    // ==========  Crypto Helper Functions  ============
    // =================================================

    static byte[] longToBytes(long a, long b) {
        byte[] out = new byte[16];
        for (int i = 0; i < 8; i++) {
            out[i]     = (byte)(a >>> (56 - i * 8));
            out[i + 8] = (byte)(b >>> (56 - i * 8));
        }
        return out;
    }
    static byte[] aesEcbEncrypt(byte[] key, byte[] block) {
        try {
            Cipher c = Cipher.getInstance("AES/ECB/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
            return c.doFinal(block);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
    static long beLong(byte[] b, int off) {
        long r = 0;
        for (int i = 0; i < 8; i++) r = (r << 8) | (b[off + i] & 0xFF);
        return r;
    }
    static byte[] gf128Mul(byte[] X, byte[] Y) {
        byte[] Z = new byte[16];
        byte[] V = Y.clone();
        for (int i = 0; i < 128; i++) {
            int bit = (X[i / 8] >>> (7 - (i % 8))) & 1;
            if (bit != 0) for (int j = 0; j < 16; j++) Z[j] ^= V[j];
            int lsb = V[15] & 1;
            for (int j = 15; j > 0; j--)
                V[j] = (byte)((V[j] >>> 1) | ((V[j - 1] & 1) << 7));
            V[0] = (byte)(V[0] >>> 1);
            if (lsb != 0) V[0] ^= (byte)0xE1;
        }
        return Z;
    }
    static long load64LE(byte[] b, int off) {
        return ((long)(b[off] & 0xFF))
             | ((long)(b[off + 1] & 0xFF) << 8)
             | ((long)(b[off + 2] & 0xFF) << 16)
             | ((long)(b[off + 3] & 0xFF) << 24)
             | ((long)(b[off + 4] & 0xFF) << 32)
             | ((long)(b[off + 5] & 0xFF) << 40)
             | ((long)(b[off + 6] & 0xFF) << 48)
             | ((long)(b[off + 7] & 0xFF) << 56);
    }
    private static final long[] BLAKE2B_IV = {0x6a09e667f3bcc908L,0xbb67ae8584caa73bL,0x3c6ef372fe94f82bL,0xa54ff53a5f1d36f1L,
            0x510e527fade682d1L,0x9b05688c2b3e6c1fL,0x1f83d9abfb41bd6bL,0x5be0cd19137e2179L};
    private static final int[][] BLAKE2B_SIGMA = {
            {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15},
            {14,10,4,8,9,15,13,6,1,12,0,2,11,7,5,3},
            {11,8,12,0,5,2,15,13,10,14,3,6,7,1,9,4},
            {7,9,3,1,13,12,11,14,2,6,5,10,4,0,15,8},
            {9,0,5,7,2,4,10,15,14,1,11,12,6,8,3,13},
            {2,12,6,10,0,11,8,3,4,13,7,5,15,14,1,9},
            {12,5,1,15,14,13,4,10,0,7,6,3,9,2,8,11},
            {13,11,7,14,12,1,3,9,5,0,15,4,8,6,2,10},
            {6,15,14,9,11,3,0,8,12,2,13,7,1,4,10,5},
            {10,2,8,4,7,6,1,5,15,11,9,14,3,12,13,0}};
    private static final int[] KECCAK_RHO = {
            0,1,62,28,27,36,44,6,55,20,3,10,43,25,39,41,45,15,21,8,18,2,61,56,14};
    private static final long[] KECCAK_RC = {
            0x0000000000000001L,0x0000000000008082L,0x800000000000808AL,0x8000000080008000L,
            0x000000000000808BL,0x0000000080000001L,0x8000000080008081L,0x8000000000008009L,
            0x000000000000008AL,0x0000000000000088L,0x0000000080008009L,0x000000008000000AL,
            0x000000008000808BL,0x800000000000008BL,0x8000000000008089L,0x8000000000008003L,
            0x8000000000008002L,0x8000000000000080L,0x000000000000800AL,0x800000008000000AL,
            0x8000000080008081L,0x8000000000008080L,0x0000000080000001L,0x8000000080008008L};
    private static final byte[] KMAC_N = "KMAC".getBytes();
    static void blake2bG(long[] v, int a, int b, int c, int d, long x, long y) {
        v[a] = v[a] + v[b] + x;
        v[d] = Long.rotateRight(v[d] ^ v[a], 32);
        v[c] = v[c] + v[d];
        v[b] = Long.rotateRight(v[b] ^ v[c], 24);
        v[a] = v[a] + v[b] + y;
        v[d] = Long.rotateRight(v[d] ^ v[a], 16);
        v[c] = v[c] + v[d];
        v[b] = Long.rotateRight(v[b] ^ v[c], 63);
    }
    static byte[] blake2b(byte[] message, byte[] key, int outLen) {
        long[] h = BLAKE2B_IV.clone();
        h[0] ^= 0x01010000L ^ ((long)key.length << 8) ^ outLen;

        byte[] input;
        if (key.length > 0) {
            byte[] keyBlock = new byte[128];
            System.arraycopy(key, 0, keyBlock, 0, Math.min(key.length, 128));
            input = new byte[128 + message.length];
            System.arraycopy(keyBlock, 0, input, 0, 128);
            System.arraycopy(message, 0, input, 128, message.length);
        } else {
            input = message;
        }

        int numBlocks = Math.max(1, (input.length + 127) / 128);
        long t = 0;
        for (int b = 0; b < numBlocks; b++) {
            int start = b * 128;
            int end = Math.min(start + 128, input.length);
            int blockLen = end - start;
            t += blockLen;

            byte[] block = new byte[128];
            if (blockLen > 0) System.arraycopy(input, start, block, 0, blockLen);

            long[] v = new long[16];
            System.arraycopy(h, 0, v, 0, 8);
            System.arraycopy(BLAKE2B_IV, 0, v, 8, 8);
            v[12] ^= t;
            v[13] ^= 0;
            if (b == numBlocks - 1) v[14] = ~v[14];

            for (int r = 0; r < 12; r++) {
                int[] s = BLAKE2B_SIGMA[r % 10];
                blake2bG(v, 0, 4,  8, 12, load64LE(block, s[0]  * 8), load64LE(block, s[1]  * 8));
                blake2bG(v, 1, 5,  9, 13, load64LE(block, s[2]  * 8), load64LE(block, s[3]  * 8));
                blake2bG(v, 2, 6, 10, 14, load64LE(block, s[4]  * 8), load64LE(block, s[5]  * 8));
                blake2bG(v, 3, 7, 11, 15, load64LE(block, s[6]  * 8), load64LE(block, s[7]  * 8));
                blake2bG(v, 0, 5, 10, 15, load64LE(block, s[8]  * 8), load64LE(block, s[9]  * 8));
                blake2bG(v, 1, 6, 11, 12, load64LE(block, s[10] * 8), load64LE(block, s[11] * 8));
                blake2bG(v, 2, 7,  8, 13, load64LE(block, s[12] * 8), load64LE(block, s[13] * 8));
                blake2bG(v, 3, 4,  9, 14, load64LE(block, s[14] * 8), load64LE(block, s[15] * 8));
            }
            for (int i = 0; i < 8; i++) h[i] ^= v[i] ^ v[i + 8];
        }

        byte[] out = new byte[outLen];
        for (int i = 0; i < outLen && i < 64; i++) {
            out[i] = (byte)(h[i / 8] >>> (8 * (i % 8)));
        }
        return out;
    }
    static byte[] sm3(byte[] message) {
        int[] V = {0x7380166f, 0x4914b2b9, 0x172442d7, 0xda8a0600,
                   0xa96f30bc, 0x163138aa, 0xe38dee4d, 0xb0fb0e4e};

        long bitLen = (long)message.length * 8;
        int padLen = ((message.length + 8) / 64 + 1) * 64;
        byte[] padded = new byte[padLen];
        System.arraycopy(message, 0, padded, 0, message.length);
        padded[message.length] = (byte)0x80;
        for (int i = 0; i < 8; i++) padded[padLen - 1 - i] = (byte)(bitLen >>> (i * 8));

        for (int blk = 0; blk < padLen; blk += 64) {
            int[] W = new int[68];
            for (int i = 0; i < 16; i++) {
                W[i] = ((padded[blk + i * 4] & 0xFF) << 24)
                     | ((padded[blk + i * 4 + 1] & 0xFF) << 16)
                     | ((padded[blk + i * 4 + 2] & 0xFF) << 8)
                     |  (padded[blk + i * 4 + 3] & 0xFF);
            }
            for (int i = 16; i < 68; i++) {
                int w1 = W[i - 16] ^ W[i - 9] ^ Integer.rotateLeft(W[i - 3], 15);
                W[i] = w1 ^ Integer.rotateLeft(w1, 15) ^ Integer.rotateLeft(w1, 23)
                     ^ Integer.rotateLeft(W[i - 13], 7) ^ W[i - 6];
            }
            int[] W1 = new int[64];
            for (int i = 0; i < 64; i++) W1[i] = W[i] ^ W[i + 4];

            int A = V[0], B = V[1], C = V[2], D = V[3];
            int E = V[4], F = V[5], G = V[6], H = V[7];
            for (int j = 0; j < 64; j++) {
                int T = j < 16 ? 0x79cc4519 : 0x7a879d8a;
                int SS1 = Integer.rotateLeft(
                        Integer.rotateLeft(A, 12) + E + Integer.rotateLeft(T, j % 32), 7);
                int SS2 = SS1 ^ Integer.rotateLeft(A, 12);
                int FF = j < 16 ? (A ^ B ^ C) : ((A & B) | (A & C) | (B & C));
                int GG = j < 16 ? (E ^ F ^ G) : ((E & F) | (~E & G));
                int TT1 = FF + D + SS2 + W1[j];
                int TT2 = GG + H + SS1 + W[j];
                D = C;
                C = Integer.rotateLeft(B, 9);
                B = A;
                A = TT1;
                H = G;
                G = Integer.rotateLeft(F, 19);
                F = E;
                E = TT2 ^ Integer.rotateLeft(TT2, 9) ^ Integer.rotateLeft(TT2, 17);
            }
            V[0] ^= A; V[1] ^= B; V[2] ^= C; V[3] ^= D;
            V[4] ^= E; V[5] ^= F; V[6] ^= G; V[7] ^= H;
        }

        byte[] out = new byte[32];
        for (int i = 0; i < 8; i++) {
            out[i * 4]     = (byte)(V[i] >>> 24);
            out[i * 4 + 1] = (byte)(V[i] >>> 16);
            out[i * 4 + 2] = (byte)(V[i] >>> 8);
            out[i * 4 + 3] = (byte)V[i];
        }
        return out;
    }
    static void keccakF(long[] s) {
        for (int round = 0; round < 24; round++) {
            long[] C = new long[5];
            for (int x = 0; x < 5; x++)
                C[x] = s[x] ^ s[x + 5] ^ s[x + 10] ^ s[x + 15] ^ s[x + 20];
            long[] D = new long[5];
            for (int x = 0; x < 5; x++)
                D[x] = C[(x + 4) % 5] ^ Long.rotateLeft(C[(x + 1) % 5], 1);
            for (int x = 0; x < 5; x++)
                for (int y = 0; y < 5; y++) s[x + y * 5] ^= D[x];

            long[] B = new long[25];
            for (int x = 0; x < 5; x++)
                for (int y = 0; y < 5; y++) {
                    int nx = y, ny = (2 * x + 3 * y) % 5;
                    B[nx + ny * 5] = Long.rotateLeft(s[x + y * 5], KECCAK_RHO[x + 5 * y]);
                }
            for (int x = 0; x < 5; x++)
                for (int y = 0; y < 5; y++)
                    s[x + y * 5] = B[x + y * 5] ^ (~B[(x + 1) % 5 + y * 5] & B[(x + 2) % 5 + y * 5]);
            s[0] ^= KECCAK_RC[round];
        }
    }
    static byte[] leftEncode(long x) {
        int n = 1;
        long v = x;
        while ((v >>>= 8) != 0) n++;
        byte[] out = new byte[n + 1];
        out[0] = (byte)n;
        for (int i = 0; i < n; i++) out[1 + i] = (byte)(x >>> (8 * (n - 1 - i)));
        return out;
    }
    static byte[] rightEncode(long x) {
        int n = 1;
        long v = x;
        while ((v >>>= 8) != 0) n++;
        byte[] out = new byte[n + 1];
        for (int i = 0; i < n; i++) out[i] = (byte)(x >>> (8 * (n - 1 - i)));
        out[n] = (byte)n;
        return out;
    }
    static byte[] encodeString(byte[] s) {
        byte[] len = leftEncode((long)s.length * 8);
        return concat(len, s);
    }
    static byte[] bytepad(byte[] x, int w) {
        byte[] left = leftEncode(w);
        byte[] tmp = concat(left, x);
        int pad = (w - (tmp.length % w)) % w;
        byte[] out = new byte[tmp.length + pad];
        System.arraycopy(tmp, 0, out, 0, tmp.length);
        return out;
    }
    static byte[] cshake(int rate, byte[] msg, int outLen, byte[] N, byte[] S) {
        byte[] prefix = bytepad(concat(encodeString(N), encodeString(S)), rate);
        byte[] input = concat(prefix, msg);

        long[] state = new long[25];
        int pos = 0;
        while (input.length - pos >= rate) {
            for (int i = 0; i < rate; i++)
                state[i / 8] ^= ((long)(input[pos + i] & 0xFF)) << (8 * (i % 8));
            keccakF(state);
            pos += rate;
        }
        int rem = input.length - pos;
        for (int i = 0; i < rem; i++)
            state[i / 8] ^= ((long)(input[pos + i] & 0xFF)) << (8 * (i % 8));

        byte dsep = (N.length == 0 && S.length == 0) ? (byte)0x1F : (byte)0x04;
        state[rem / 8] ^= ((long)dsep) << (8 * (rem % 8));
        state[(rate - 1) / 8] ^= ((long)0x80) << (8 * ((rate - 1) % 8));
        keccakF(state);

        byte[] out = new byte[outLen];
        int written = 0;
        while (written < outLen) {
            for (int i = 0; i < rate && written < outLen; i++)
                out[written++] = (byte)(state[i / 8] >>> (8 * (i % 8)));
            if (written < outLen) keccakF(state);
        }
        return out;
    }
    static byte[] kmac128(byte[] key, byte[] msg, int outBytes) {
        byte[] newX = concat(bytepad(encodeString(key), 168),
                             concat(msg, rightEncode((long)outBytes * 8)));
        return cshake(168, newX, outBytes, KMAC_N, new byte[0]);
    }
    static byte[] kmac256(byte[] key, byte[] msg, int outBytes) {
        byte[] newX = concat(bytepad(encodeString(key), 136),
                             concat(msg, rightEncode((long)outBytes * 8)));
        return cshake(136, newX, outBytes, KMAC_N, new byte[0]);
    }
    static byte[] hmac(byte[] key, byte[] msg, int blockSize, Function<byte[], byte[]> hash) {
        byte[] k = key.length > blockSize ? hash.apply(key) : key;
        byte[] kBlock = new byte[blockSize];
        System.arraycopy(k, 0, kBlock, 0, k.length);
        byte[] ipad = new byte[blockSize];
        byte[] opad = new byte[blockSize];
        for (int i = 0; i < blockSize; i++) {
            ipad[i] = (byte)(kBlock[i] ^ 0x36);
            opad[i] = (byte)(kBlock[i] ^ 0x5C);
        }
        byte[] inner = hash.apply(concat(ipad, msg));
        return hash.apply(concat(opad, inner));
    }
    static byte[] hmacSha256(byte[] key, byte[] msg) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(msg);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    static byte[] hmacSha512(byte[] key, byte[] msg) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key, "HmacSHA512"));
            return mac.doFinal(msg);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    static byte[] hmacBlake2b(byte[] key, byte[] msg) {
        return hmac(key, msg, 128, b -> blake2b(b, new byte[0], 64));
    }
    static byte[] hmacSm3(byte[] key, byte[] msg) {
        return hmac(key, msg, 64, RngAlgorithms::sm3);
    }
}