// Custom error classes for LoqaExpoDsp module
/**
 * Base error class for all LoqaExpoDsp errors
 *
 * Provides a consistent error structure with error codes and additional details
 * for debugging and error handling.
 */
export class LoqaExpoDspError extends Error {
    code;
    details;
    /**
     * Creates a new LoqaExpoDspError
     * @param message - Human-readable error message
     * @param code - Error code for programmatic error handling
     * @param details - Additional error details (optional)
     */
    constructor(message, code, details) {
        super(message);
        this.code = code;
        this.details = details;
        this.name = 'LoqaExpoDspError';
    }
}
/** @deprecated Use LoqaExpoDspError instead */
export const LoqaAudioDspError = LoqaExpoDspError;
/**
 * Error thrown when input validation fails
 *
 * This error indicates that the provided input parameters did not meet the
 * required constraints (e.g., buffer size, sample rate range, FFT size).
 */
export class ValidationError extends LoqaExpoDspError {
    /**
     * Creates a new ValidationError
     * @param message - Description of the validation failure
     * @param details - Additional context (e.g., invalid values, expected ranges)
     */
    constructor(message, details) {
        super(message, 'VALIDATION_ERROR', details);
        this.name = 'ValidationError';
    }
}
/**
 * Error thrown when native module operations fail
 *
 * This error wraps errors that occur in the native iOS or Android code,
 * providing context about the failure and suggestions for resolution.
 */
export class NativeModuleError extends LoqaExpoDspError {
    /**
     * Creates a new NativeModuleError
     * @param message - Description of the native module failure
     * @param details - Additional context (e.g., original native error)
     */
    constructor(message, details) {
        super(message, 'NATIVE_MODULE_ERROR', details);
        this.name = 'NativeModuleError';
    }
}
//# sourceMappingURL=errors.js.map