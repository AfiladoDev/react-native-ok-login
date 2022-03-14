import * as React from 'react';

import { View, Text, TouchableOpacity } from 'react-native';
import { initialize, login, logout } from 'react-native-ok-login';

export default function App() {
  React.useEffect(() => {
    initialize('0000000000', 'AAAAAAAAAAAAAAAAA');
  }, []);

  return (
    <View
      style={{ flexDirection: 'column', alignItems: 'stretch', padding: 50 }}
    >
      <TouchableOpacity
        onPress={() => {
          login(['VALUABLE_ACCESS'])
            .then((result) => console.log('result 2222', result))
            .catch((err) => console.log(1111, err));
        }}
      >
        <Text>Login</Text>
      </TouchableOpacity>
      <TouchableOpacity
        onPress={() => {
          logout();
        }}
      >
        <Text>logout</Text>
      </TouchableOpacity>
    </View>
  );
}
