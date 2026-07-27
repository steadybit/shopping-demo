import type { JestConfigWithTsJest } from 'ts-jest';

const config: JestConfigWithTsJest = {
  preset: 'ts-jest/presets/default-esm',
  testEnvironment: 'jsdom',
  extensionsToTreatAsEsm: ['.ts', '.tsx'],
  globals: {
    TextEncoder: TextEncoder,
    TextDecoder: TextDecoder,
  },
  transform: {
    '^.+\\.tsx?$': ['ts-jest', { useESM: true }],
    '^.+\\.m?jsx?$': ['ts-jest', { useESM: true, tsconfig: { allowJs: true } }],
  },
  moduleNameMapper: {
    "\\.(scss|sass|css)$": "identity-obj-proxy",
    "\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$": "<rootDir>/src/__mocks__/fileMock.ts",
  },
  transformIgnorePatterns: ['/node_modules/(?!(uuid|react-router|cookie-es)/)'],
  setupFilesAfterEnv: ['<rootDir>/src/jest-setup.ts']
};

export default config;
