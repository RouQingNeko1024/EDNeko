#version 120

uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;

varying vec2 texCoord;

uniform vec2 OutSize;
uniform vec3 Phosphor;

void main() {
    vec4 currentColor = texture2D(DiffuseSampler, texCoord);
    vec4 prevColor = texture2D(PrevSampler, texCoord);
    
    float strength = Phosphor.r;
    
    vec3 result = currentColor.rgb;
    
    vec2 texelSize = vec2(1.0 / OutSize.x, 1.0 / OutSize.y);
    
    float blurAmount = strength * 12.0;
    
    float sampleWeight = strength * 0.06;
    float totalWeight = 1.0;
    
    vec2 offset;
    
    offset = vec2(1.0, 0.0) * texelSize * blurAmount;
    result += texture2D(PrevSampler, texCoord + offset).rgb * sampleWeight;
    totalWeight += sampleWeight;
    
    offset = vec2(-1.0, 0.0) * texelSize * blurAmount;
    result += texture2D(PrevSampler, texCoord + offset).rgb * sampleWeight;
    totalWeight += sampleWeight;
    
    offset = vec2(0.0, 1.0) * texelSize * blurAmount;
    result += texture2D(PrevSampler, texCoord + offset).rgb * sampleWeight;
    totalWeight += sampleWeight;
    
    offset = vec2(0.0, -1.0) * texelSize * blurAmount;
    result += texture2D(PrevSampler, texCoord + offset).rgb * sampleWeight;
    totalWeight += sampleWeight;
    
    offset = vec2(0.866, 0.5) * texelSize * blurAmount;
    result += texture2D(PrevSampler, texCoord + offset).rgb * sampleWeight;
    totalWeight += sampleWeight;
    
    offset = vec2(-0.866, 0.5) * texelSize * blurAmount;
    result += texture2D(PrevSampler, texCoord + offset).rgb * sampleWeight;
    totalWeight += sampleWeight;
    
    offset = vec2(0.866, -0.5) * texelSize * blurAmount;
    result += texture2D(PrevSampler, texCoord + offset).rgb * sampleWeight;
    totalWeight += sampleWeight;
    
    offset = vec2(-0.866, -0.5) * texelSize * blurAmount;
    result += texture2D(PrevSampler, texCoord + offset).rgb * sampleWeight;
    totalWeight += sampleWeight;
    
    offset = vec2(0.5, 0.866) * texelSize * blurAmount;
    result += texture2D(PrevSampler, texCoord + offset).rgb * sampleWeight;
    totalWeight += sampleWeight;
    
    offset = vec2(-0.5, 0.866) * texelSize * blurAmount;
    result += texture2D(PrevSampler, texCoord + offset).rgb * sampleWeight;
    totalWeight += sampleWeight;
    
    offset = vec2(0.5, -0.866) * texelSize * blurAmount;
    result += texture2D(PrevSampler, texCoord + offset).rgb * sampleWeight;
    totalWeight += sampleWeight;
    
    offset = vec2(-0.5, -0.866) * texelSize * blurAmount;
    result += texture2D(PrevSampler, texCoord + offset).rgb * sampleWeight;
    totalWeight += sampleWeight;
    
    result = result / totalWeight;
    
    result = mix(currentColor.rgb, result, strength * 0.7);
    
    gl_FragColor = vec4(result, 1.0);
}
