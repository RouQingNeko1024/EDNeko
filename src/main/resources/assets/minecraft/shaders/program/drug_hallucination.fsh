#version 120

uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;

varying vec2 texCoord;

uniform vec2 OutSize;
uniform vec3 Phosphor;

void main() {
    vec2 texelSize = vec2(1.0 / OutSize.x, 1.0 / OutSize.y);
    
    vec4 currentColor = texture2D(DiffuseSampler, texCoord);
    vec4 prevColor = texture2D(PrevSampler, texCoord);
    
    float strength = Phosphor.r;
    
    vec2 velocity = vec2(0.0);
    
    vec4 left   = texture2D(PrevSampler, texCoord - vec2(texelSize.x, 0.0));
    vec4 right  = texture2D(PrevSampler, texCoord + vec2(texelSize.x, 0.0));
    vec4 top    = texture2D(PrevSampler, texCoord - vec2(0.0, texelSize.y));
    vec4 bottom = texture2D(PrevSampler, texCoord + vec2(0.0, texelSize.y));
    
    velocity.x = dot(abs(right.rgb - left.rgb), vec3(0.333));
    velocity.y = dot(abs(bottom.rgb - top.rgb), vec3(0.333));
    
    vec4 topLeft     = texture2D(PrevSampler, texCoord - texelSize);
    vec4 topRight    = texture2D(PrevSampler, texCoord + vec2(texelSize.x, -texelSize.y));
    vec4 bottomLeft  = texture2D(PrevSampler, texCoord + vec2(-texelSize.x, texelSize.y));
    vec4 bottomRight = texture2D(PrevSampler, texCoord + texelSize);
    
    float diagX = dot(abs(topRight.rgb - bottomLeft.rgb), vec3(0.333));
    float diagY = dot(abs(bottomRight.rgb - topLeft.rgb), vec3(0.333));
    
    velocity.x += diagX * 0.707;
    velocity.y += diagY * 0.707;
    
    float speed = length(velocity);
    
    if (speed > 0.001) {
        velocity = normalize(velocity) * speed * strength * 30.0;
    }
    
    vec3 result = currentColor.rgb;
    
    float blurRadius = strength * 25.0;
    
    for (int i = 1; i <= 16; i++) {
        float t = float(i) / 16.0;
        float decay = 1.0 - t * 0.5;
        
        vec2 sampleOffset = velocity * t * texelSize * blurRadius;
        vec4 sampleColor = texture2D(PrevSampler, texCoord + sampleOffset);
        
        float weight = decay * strength * 0.5;
        result = mix(result, sampleColor.rgb, weight);
    }
    
    result = mix(currentColor.rgb, result, 0.8);
    
    gl_FragColor = vec4(result, 1.0);
}
