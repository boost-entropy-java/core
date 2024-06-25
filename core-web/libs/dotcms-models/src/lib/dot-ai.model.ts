import { DotCMSContentlet } from './dot-contentlet.model';

export const enum PromptType {
    INPUT = 'input',
    AUTO = 'auto'
}

export interface AiPluginResponse {
    id: string;
    object: string;
    created: number;
    model: string;
    choices: Choice[];
    usage: Usage;
    system_fingerprint: null;
    totalTime: string;
    error?: DotAiError;
}

interface Choice {
    index: number;
    message: Message;
    finish_reason: string;
}
interface Message {
    role: string;
    content: string;
}
interface Usage {
    prompt_tokens: number;
    completion_tokens: number;
    total_tokens: number;
}

/**
 * Represents the response received from the DotAI Image API.
 */
export interface DotAIImageResponse {
    originalPrompt: string;
    response: string;
    revised_prompt: string;
    tempFileName: string;
    url: string;
}

/**
 * Represents an AI image prompt and the possible config options.
 */
export interface AIImagePrompt {
    text: string;
    type: PromptType;
    size: DotAIImageOrientation;
}

/**
 Represents the response received from the DotAI Image API plus
 the contentle generated by DotCMS with the response data
 */
export interface DotAIImageContent extends DotAIImageResponse {
    contentlet: DotCMSContentlet;
}

/**
 * Represents the response and request of a generated AI image,
 * to keep sync when the user change between the gallery and the form
 */
export interface DotGeneratedAIImage {
    request: AIImagePrompt;
    response: DotAIImageContent;
    error?: string;
}

/**
 * Represents the possible orientations of a Dot AI image.
 */
export const enum DotAIImageOrientation {
    HORIZONTAL = '1792x1024',
    SQUARE = '1024x1024',
    VERTICAL = '1024x1792'
}

export interface DotAICompletionsConfig {
    apiImageUrl: string;
    apiKey: string;
    apiUrl: string;
    availableModels: string[];
    configHost: string;
    imageModel: string;
    imagePrompt: string;
    imageSize: string;
    model: string;
    rolePrompt: string;
    textPrompt: string;
}

export interface DotAiError {
    code: string;
    message: string;
    param: string;
    type: string;
}