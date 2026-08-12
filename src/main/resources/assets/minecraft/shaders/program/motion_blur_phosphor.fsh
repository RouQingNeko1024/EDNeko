#version 120

uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;

varying vec2 texCoord;

uniform vec3 Phosphor;

void main() {
    vec4 currentColor = texture2D(DiffuseSampler, texCoord);
    vec4 prevColor = texture2D(PrevSampler, texCoord);
    
    float strength = Phosphor.r;
    
    vec3 result = currentColor.rgb + prevColor.rgb * strength;
    
    result = clamp(result, 0.0, 1.0);
    
    gl_FragColor = vec4(result, 1.0);
}
