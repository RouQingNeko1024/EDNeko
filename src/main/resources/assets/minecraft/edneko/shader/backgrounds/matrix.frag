#version 120

#ifdef GL_ES
precision lowp float;
#endif

uniform float iTime;
uniform vec2 iResolution;

float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    
    vec3 color = vec3(0.0);
    
    float columns = 80.0;
    float speed = 2.0;
    
    float col = floor(uv.x * columns);
    float randCol = random(vec2(col, 0.0));
    
    float y = fract(uv.y + iTime * speed * randCol + randCol * 10.0);
    
    float charIntensity = step(0.1, random(vec2(col, floor(uv.y * 30.0 - iTime * speed * randCol))));
    
    float fade = smoothstep(0.0, 0.5, y) * smoothstep(1.0, 0.5, y);
    fade *= 0.5 + 0.5 * sin(iTime * 3.0 + randCol * 10.0);
    
    vec3 matrixGreen = vec3(0.0, 0.8, 0.2);
    vec3 brightGreen = vec3(0.3, 1.0, 0.5);
    
    color = mix(matrixGreen, brightGreen, fade) * fade * charIntensity;
    
    color += vec3(0.0, 0.05, 0.02);
    
    fragColor = vec4(color, 1.0);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}
