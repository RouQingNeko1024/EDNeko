#version 120

#ifdef GL_ES
precision lowp float;
#endif

uniform float iTime;
uniform vec2 iResolution;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    
    vec3 color = vec3(0.05, 0.05, 0.1);
    
    float numDrops = 100.0;
    
    for(float i = 0.0; i < numDrops; i++) {
        float randX = hash(vec2(i, 0.0));
        float randSpeed = 0.5 + hash(vec2(i, 1.0)) * 1.5;
        float randLength = 0.05 + hash(vec2(i, 2.0)) * 0.15;
        
        float x = randX;
        float y = fract(hash(vec2(i, 3.0)) - iTime * randSpeed * 0.3);
        
        float dist = length(vec2(uv.x - x, uv.y - y));
        
        float drop = smoothstep(randLength, 0.0, dist);
        drop *= smoothstep(0.0, 0.01, y);
        
        color += vec3(0.3, 0.4, 0.5) * drop * 0.5;
    }
    
    float splash = hash(uv * 100.0 + floor(iTime * 10.0)) * 0.1;
    color += vec3(0.2, 0.3, 0.4) * splash * step(0.95, uv.y);
    
    fragColor = vec4(color, 1.0);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}
