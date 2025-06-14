<?php

class AIService {
    private $config;
    
    public function __construct($config) {
        $this->config = $config;
    }
    
    public function isEnabled() {
        return $this->config['ai']['enabled'] ?? false;
    }
    
    public function getAvailableProviders() {
        $providers = [];
        foreach ($this->config['ai']['providers'] as $name => $provider) {
            if ($provider['enabled'] && !empty($provider['api_key'])) {
                $providers[$name] = [
                    'name' => $name,
                    'models' => $provider['models']
                ];
            }
        }
        return $providers;
    }
    
    public function translate($text, $sourceLanguage, $targetLanguage, $context = [], $provider = null, $model = null) {
        if (!$this->isEnabled()) {
            throw new Exception('AI features are not enabled');
        }
        
        $provider = $provider ?? $this->config['ai']['default_provider'];
        $model = $model ?? $this->config['ai']['default_model'];
        
        $providerConfig = $this->config['ai']['providers'][$provider] ?? null;
        if (!$providerConfig || !$providerConfig['enabled']) {
            throw new Exception("Provider $provider is not available");
        }
        
        $prompt = $this->buildTranslationPrompt($text, $sourceLanguage, $targetLanguage, $context);
        
        return $this->makeAIRequest($provider, $model, $prompt, $providerConfig);
    }
    
    public function proofread($text, $language, $context = [], $provider = null, $model = null) {
        if (!$this->isEnabled()) {
            throw new Exception('AI features are not enabled');
        }
        
        $provider = $provider ?? $this->config['ai']['default_provider'];
        $model = $model ?? $this->config['ai']['default_model'];
        
        $providerConfig = $this->config['ai']['providers'][$provider] ?? null;
        if (!$providerConfig || !$providerConfig['enabled']) {
            throw new Exception("Provider $provider is not available");
        }
        
        $prompt = $this->buildProofreadingPrompt($text, $language, $context);
        
        return $this->makeAIRequest($provider, $model, $prompt, $providerConfig, true);
    }
    
    private function buildTranslationPrompt($text, $sourceLanguage, $targetLanguage, $context) {
        $contextStr = '';
        if (!empty($context)) {
            $contextStr = "\n\nContext (related strings):\n";
            foreach ($context as $item) {
                $contextStr .= "- Key: {$item['key']}, {$sourceLanguage}: \"{$item['source']}\", {$targetLanguage}: \"{$item['target']}\"\n";
            }
        }
        
        return "Translate the following text from {$sourceLanguage} to {$targetLanguage}. This is for a mobile/desktop application localization.

Source text: \"{$text}\"
Source language: {$sourceLanguage}
Target language: {$targetLanguage}
{$contextStr}

Instructions:
- Provide only the translated text, no explanations
- Maintain the same tone and style
- Consider the context of mobile/desktop application UI
- Keep placeholders and formatting intact if any
- If it's a technical term or brand name, consider if it should remain untranslated

Translation:";
    }
    
    private function buildProofreadingPrompt($text, $language, $context) {
        $contextStr = '';
        if (!empty($context)) {
            $contextStr = "\n\nContext (related strings in the same language):\n";
            foreach ($context as $item) {
                $contextStr .= "- Key: {$item['key']}, Text: \"{$item['text']}\"\n";
            }
        }
        
        return "Review the following localized text for a mobile/desktop application. Evaluate the quality and provide feedback.

Text to review: \"{$text}\"
Language: {$language}
{$contextStr}

Please respond with a JSON object containing:
{
  \"status\": \"good\" | \"wording\" | \"issue\",
  \"feedback\": \"explanation of any issues or suggestions\"
}

Status meanings:
- \"good\": The text is well-written and appropriate
- \"wording\": The text is understandable but could be improved with better wording
- \"issue\": There are serious problems (grammar, unclear meaning, inappropriate tone, etc.)

Consider:
- Grammar and spelling
- Clarity and naturalness
- Appropriateness for UI context
- Consistency with typical app terminology
- Cultural appropriateness

Response:";
    }
    
    private function makeAIRequest($provider, $model, $prompt, $providerConfig, $expectJson = false) {
        switch ($provider) {
            case 'openai':
            case 'openai_compatible':
                return $this->makeOpenAIRequest($model, $prompt, $providerConfig, $expectJson);
            case 'anthropic':
                return $this->makeAnthropicRequest($model, $prompt, $providerConfig, $expectJson);
            default:
                throw new Exception("Unsupported provider: $provider");
        }
    }
    
    private function makeOpenAIRequest($model, $prompt, $config, $expectJson = false) {
        $headers = [
            'Content-Type: application/json',
            'Authorization: Bearer ' . $config['api_key']
        ];
        
        $data = [
            'model' => $model,
            'messages' => [
                [
                    'role' => 'user',
                    'content' => $prompt
                ]
            ],
            'max_tokens' => 500,
            'temperature' => 0.3
        ];
        
        if ($expectJson) {
            $data['response_format'] = ['type' => 'json_object'];
        }
        
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $config['base_url'] . '/chat/completions');
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 30);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        
        if ($response === false || $httpCode !== 200) {
            throw new Exception('AI request failed: HTTP ' . $httpCode);
        }
        
        $result = json_decode($response, true);
        if (!$result || !isset($result['choices'][0]['message']['content'])) {
            throw new Exception('Invalid AI response format');
        }
        
        $content = trim($result['choices'][0]['message']['content']);
        
        if ($expectJson) {
            $decoded = json_decode($content, true);
            if (!$decoded) {
                throw new Exception('Invalid JSON response from AI');
            }
            return $decoded;
        }
        
        return $content;
    }
    
    private function makeAnthropicRequest($model, $prompt, $config, $expectJson = false) {
        $headers = [
            'Content-Type: application/json',
            'x-api-key: ' . $config['api_key'],
            'anthropic-version: 2023-06-01'
        ];
        
        if ($expectJson) {
            $prompt .= "\n\nIMPORTANT: Respond only with valid JSON, no other text.";
        }
        
        $data = [
            'model' => $model,
            'max_tokens' => 500,
            'temperature' => 0.3,
            'messages' => [
                [
                    'role' => 'user',
                    'content' => $prompt
                ]
            ]
        ];
        
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $config['base_url'] . '/v1/messages');
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 30);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        
        if ($response === false || $httpCode !== 200) {
            throw new Exception('AI request failed: HTTP ' . $httpCode);
        }
        
        $result = json_decode($response, true);
        if (!$result || !isset($result['content'][0]['text'])) {
            throw new Exception('Invalid AI response format');
        }
        
        $content = trim($result['content'][0]['text']);
        
        if ($expectJson) {
            $decoded = json_decode($content, true);
            if (!$decoded) {
                throw new Exception('Invalid JSON response from AI');
            }
            return $decoded;
        }
        
        return $content;
    }
    
    public function buildContext($currentKey, $allStrings, $language, $maxItems = 5) {
        $context = [];
        $count = 0;
        
        foreach ($allStrings as $key => $stringData) {
            if ($count >= $maxItems || $key === $currentKey) continue;
            
            if (isset($stringData['localizations'][$language]['stringUnit']['value'])) {
                $context[] = [
                    'key' => $key,
                    'text' => $stringData['localizations'][$language]['stringUnit']['value']
                ];
                $count++;
            }
        }
        
        return $context;
    }
    
    public function buildTranslationContext($currentKey, $allStrings, $sourceLanguage, $targetLanguage, $maxItems = 5) {
        $context = [];
        $count = 0;
        
        foreach ($allStrings as $key => $stringData) {
            if ($count >= $maxItems || $key === $currentKey) continue;
            
            $sourceText = $stringData['localizations'][$sourceLanguage]['stringUnit']['value'] ?? '';
            $targetText = $stringData['localizations'][$targetLanguage]['stringUnit']['value'] ?? '';
            
            if (!empty($sourceText) && !empty($targetText)) {
                $context[] = [
                    'key' => $key,
                    'source' => $sourceText,
                    'target' => $targetText
                ];
                $count++;
            }
        }
        
        return $context;
    }
}
?>