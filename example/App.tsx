import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { FFTScreen } from './src/screens/FFTScreen';
import { PitchScreen } from './src/screens/PitchScreen';
import { FormantScreen } from './src/screens/FormantScreen';
import { SpectrumScreen } from './src/screens/SpectrumScreen';

const Tab = createBottomTabNavigator();

export default function App() {
  return (
    <NavigationContainer>
      <Tab.Navigator
        screenOptions={{
          headerShown: false,
          tabBarActiveTintColor: '#4A90E2',
          tabBarInactiveTintColor: '#95A5A6',
          tabBarStyle: {
            paddingBottom: 8,
            paddingTop: 8,
            height: 60,
          },
          tabBarLabelStyle: {
            fontSize: 12,
            fontWeight: '600',
          },
        }}
      >
        <Tab.Screen
          name="FFT"
          component={FFTScreen}
          options={{
            tabBarLabel: 'FFT',
            title: 'FFT Analyzer',
          }}
        />
        <Tab.Screen
          name="Pitch"
          component={PitchScreen}
          options={{
            tabBarLabel: 'Pitch',
            title: 'Pitch Detector',
          }}
        />
        <Tab.Screen
          name="Formants"
          component={FormantScreen}
          options={{
            tabBarLabel: 'Formants',
            title: 'Formant Extractor',
          }}
        />
        <Tab.Screen
          name="Spectrum"
          component={SpectrumScreen}
          options={{
            tabBarLabel: 'Spectrum',
            title: 'Spectral Analyzer',
          }}
        />
      </Tab.Navigator>
    </NavigationContainer>
  );
}
